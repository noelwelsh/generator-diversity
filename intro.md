# Diversity in Generator Output

## Introduction

Unit testing is the dominant approach to testing in industry. In unit testing the programmer explicitly defines inputs and expected outputs for the function under test. The test coverage of unit testing is limited by the amount of effort the programmer puts in creating distinct input and output pairs. In practice, test coverage is often low.

Property based testing, or generative testing, is a lightweight extension of unit testing that aims to increase test coverage. Instead of the programmer explicitly specifying input and output, the programmer defines a process for generating inputs, which we call a **generator**, and properties that must hold for the output. By using the computer to generate the input, many more inputs can be generated than when the programmer must specify them by hand.

Property based testing has increasing adoption within industry, with libraries available in many popular languages. However, this doesn&rsquo;t mean existing practice cannot be improved. One issue, often overlooked, is that the diversity within the generated output may be lower than expected. We will formalize measurement of diversity later; informally we are interested in the distance between the generated inputs. We hypothesize that greater input diversity will lead to increased test coverage.

There are two issues with test diversity in property based testing:

1.  A generator defines a distribution over test inputs. Most programmers are not statisticians, and in practice little attention is paid to the properties of the distribution.

2.  Randomly generated data often displays &ldquo;clumping&rdquo;: data points that are near to one another. In the property based testing setting, this is often not what we want. Rather we would like roughly equal density of generated data over the space of inputs.

In this paper we look at the task of generating diverse inputs in property based testing. We start by illustrating the problem in practice. When then discuss how we can formalize the notion of diversity, drawing on ideas from quasirandom sequences and Bayesian statistics. We then show how these ideas can be realized in a concrete implementation, which we use to verify the utility of our ideas on several real-world problems.


## The Problem of Diversity in Test Input Generation

Let&rsquo;s start with an example of a very simple property based test, taken from the Doodle library. Doodle, written in Scala, is a library for two-dimensional graphics. Color is a core abstraction in Doodle, and colors can be specified in two equivalent ways:

-   as a triple of red, green, and blue values (RGB); or
-   as a triple of hue, saturation, and lightness (HSL).

The HSL representation is easier for humans to work with, while RGB is what the computer uses to display color. Conversion between the two formats is therefore necessary, but this is not straightforward. The RGB representation defines a cube, with each component an unsigned byte. However, the HSL representation defines a cylinder, as hue is represented as an angle while saturation and lightness are both floating point numbers in the range 0.0 to 1.0.

There is a simple property that checks the conversion: converting an HSL color to RGB and back should yield (approximately, due to floating point issues) the same color. In other words, the conversion from HSL to RGB should be invertible. We can also check the conversion going the other way: from RGB to HSL to RGB. Below is a test for these properties, encoded in the ScalaCheck property-testing library.

```scala
object ColorSpec extends Properties("Color properties") {
  import doodle.arbitrary._
  import Color._

  property(".toRGBA andThen .toHSLA is the identity") = forAll { (hsla: HSLA) =>
    (hsla ~= (hsla.toRGBA.toHSLA))
  }

  property(".toHSLA andThen .toRGBA is the identity") = forAll { (rgba: RGBA) =>
    (rgba ~= (rgba.toHSLA.toRGBA))
  }
}
```

These tests rely on generators to produce HSL and RGB colors. The definitions of the HSL generator are shown below.

```scala
val angle: Gen[Angle] =
  Gen.choose(0, 360).map(_.degrees)

val normalized: Gen[Normalized] =
  Gen.choose(0.0, 1.0).map(_.normalized)

val color: Gen[HSLA] =
  for {
    h <- angle
    s <- normalized
    l <- normalized
    a <- normalized
  } yield Color.HSLA(h, s, l, a)
```


## Measuring Diversity

One way to measure diversity is to use Bayesian surprise\citep{NIPS2005_BayesianSurprise}. Given data $D$, in this case samples from a generator, and a model $M$, in this case a generator, Bayesian surprise is defined as

\begin{equation}
Surprise(D, M) = KL(P(M | D), P(M))
\end{equation}

$KL$ is the KL-divergence, defined as

\begin{equation}
KL(P(M | D), P(M)) = \int_M P(M | D)  log\big( \frac{P(M|D)}{P(M)} \big)dM
\end{equation}

There are several terms in the equations above that are standard in Bayesian statistics but it is perhaps not clear how they relate to generators. What follows is brief sketch of Bayesian statistics.

Let&rsquo;s assume we have some way of generating data, our generator, which assigns a probability to each possible output. In other words, the generator defines a probability distribution. In keeping with the definitions above, we&rsquo;ll label our generator $M$, and the data $D$. The generator defines $P(D | M)$, the probability of the data given the generator.

In the Bayesian view we don&rsquo;t commit to single generator but instead consider a distribution over generators $P(M)$. This is known as the **prior distribution**. When we see data we can update this distribution to produce $P(M | D)$, the distribution over generators given data, known as the **posterior distribution**. How do we compute the posterior? Bayes theorem tells us that

\begin{equation}
P(M | D) = \frac{P(D|M)P(M)}{P(D)}
\end{equation}

where

\begin{equation}
P(D) = \int_M P(D|M)P(M)
\end{equation}

This tells us in theory how we can compute the posterior, but how does it work in practice? The answer is that it depends on the particular form of the prior. There is a class of prior distributions for which we easily compute the posterior. These are known as **conjugate priors**. If $M$ is choosing between discrete choices (a categorical distribution) and we use the Dirichlet distribution as our prior, the posterior will also be a Dirichlet distribution. There is also a simple rule that relates the posterior to the prior. Hence the Dirichlet is called the conjugate prior for the categorical distribution.

Note the `Select` function in **Parsing Randomness** is a categorical distribution, and thus the Dirichlet is an appropriate prior for it.

We&rsquo;ve now defined all the terms in the definition of Bayesian surprise, but it&rsquo;s worth quickly emphasizing the implications for generators. The first implication is that we don&rsquo;t view a generator as defining a particular distribution, but rather defining a family of distributions. This is actually implicit in the definition of free generators, as they do not have probabilities attached to choices. Developers do not pay any particular attention to the distribution their generators define, in my experience, so this also does not conflict with the practice of property based testing. The second implication is that the posterior makes the observed data more likely, and hence less surprising should it be generated again, and therefore the posterior acts as a summary of the data that has been generated to date.


## Generators as Distributions

Generators implicitly define a family of distributions, but we need an explicit definition if we&rsquo;re to perform the updates we need to calculate the posterior and measure Bayesian surprise.

Free generators have a single random choice operator, `Select`, for which a reasonable prior is the uniform Dirichlet over the choices given to `Select`. This, taken together with the pure computation that computes the rest of the generator, defines the prior over the generator.

There is a problem, which is the same problem that makes any static analysis of monads difficult: the functions passed to `bind` can make choices at runtime and we may not be able to statically determine what those possible choices are. (If we&rsquo;re prepared to work in the more restrictive setting of selective or applicative functors, then we have no issue.) We can think of a generator as defining a (possibly infinite) tree of choice sequences. If we&rsquo;ve never visited a particular part of the tree, any `Selects` within that part will have the prior distribution. We can distinguish other, observed, parts of the tree by the choice sequence that leads to them. Thus we can model the distribution over generators as a map from choice sequences to Dirichlet distributions.


## Calculating Bayesian Surprise

We can use a Monte Carlo scheme to approximate Bayesian surprise. All we have to do it sample distributions from the posteriors, calculate Bayesian surprise for the data $D$ on these sampled distributions, and average over all the samples. This should be sufficient for at least initial results.

The structure of our generator is called a **belief network** or **Bayesian network** and inference in a belief network is NP-hard. Belief networks are in general DAGs, while generators are trees, so there may be a special purpose algorithm we can use. I don&rsquo;t know of one and a quick search didn&rsquo;t find one.


### Algorithms for Maximising Bayesian Surprise

Now let&rsquo;s turn to algorithms that generate data that maximises Bayesian surprise. This is in general intractable, because it reduces to a search over the possibly infinite space of all possible data a generator can generate, or the equivalent search over the possibly infinite tree of choice sequences. We can instead imple ment an approximation. Below are two very simple greedy algorithms. This general problem feels very similar to the explore / exploit problem in bandit algorithms and reinforcement learning, so using ideas from there may be useful.


### &epsilon;-Greedy Algorithm

The general idea is that, when encountering a `Select` we should choose the least likely option with probability $\epsilon$ and otherwise choose another option. There are many possible definitions for &ldquo;choose another option&rdquo;. A simple example is to choose with uniform probability over all the remaining options.


<a id="org0906c9d"></a>

## Inverse Probability Greedy Algorithm

When encountering a `Select`, sample a distribution from the prior, and then construct a new distribution with probabilities proportional to the **inverse** of the probabilities in the sample. So if we sample the distribution `(0.2, 0.8)` (for a `Select` with two choices) we first calculate `(1/0.2, 1/0.8) = (5, 1.25)`, and then normalize to get `(0.8 0.2)`. We then sample a choice from this new distribution.

\bibliographystyle{kluwer}
\bibliography{/home/noel/Dropbox/Personal/liminal/references}

