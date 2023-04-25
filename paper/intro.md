# Diversity in Generator Output

## Introduction

Unit testing is the dominant approach to testing in the software industry. Unit testing requires the programmer explicitly defines inputs and expected outputs for the function under test. The test coverage of unit testing is limited by the amount of effort the programmer expends in creating distinct input and output pairs. 

Property based testing, or generative testing, is a lightweight extension of unit testing that aims to increase test coverage. Instead of the programmer explicitly specifying input and output the programmer defines a process for generating inputs, which we call a **generator**, and **properties** that must hold for the output. By using the computer to generate the input many more inputs can be generated than when the programmer must specify them by hand.

Property based testing has increasing adoption within industry, with libraries available in many popular languages. However, this doesn't mean existing practice cannot be improved. Our goal here is to make such an improvement by focusing on how generators create the test inputs. 

Property based testing libraries provides an API to construct generators. The standard approach is an implementation of the probability monad, which is simple to implement and reasonably easy to use. The generators so constructed can be viewed as functions from a source of randomness to an output of the desired type: the input to the function under test.

Ideally, the output of a generator should be highly **diverse**. We put off defining diversity here; we'll be more formal later. Intuitively we want the generated outputs to be as different from each other as possible, to maximize the chances of finding bugs. Random generation is not the best choice for achieving this goal for two reasons: 

1.  A generator defines a distribution over test inputs. Most programmers are not statisticians, and in practice little attention is paid to the properties of this distribution and in practice generators often have undesirable properties.

2.  Randomly generation is an inefficient way to explore the space of test inputs. Randomly generated data often displays ``clumping'': data points that are near to one another, or even exactly the same. 

Our focus here is on the second point, though we'll briefly touch on the first as well.

In this paper we look at the task of generating diverse inputs in property based testing. We start by illustrating the problem in practice. When then formalize the setting, and discuss how we can formalize the notion of diversity. We then discuss algorithms to increase diversity, and test these algorithms on several real-world problems.


## The Problem of Diversity in Test Input Generation

Let's start with an example of a very simple property based test, taken from the Doodle library. Doodle, written in Scala, is a library for two-dimensional graphics. Color is a core abstraction in Doodle, and colors can be specified in two equivalent ways:

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

Here, the `~=` operator denotes approximate equality.

These tests rely on generators to produce HSL and RGB colors. The definitions of the HSL generator are shown below. In words, this simply samples an angle uniformly in the range 0 to 360 degrees, and saturation and lightness uniformly in the range 0.0 to 1.0.

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

[@Fig:random-is-bad] shows 100 and 1000 colors respectively, sampled as described above except that lightness is fixed to 0.7. Fixing lightness makes no substantive change to the points we wish to illustrate, but the two-dimensional visualization is easier to interpret.

![100 and 1000 random colors, keeping lightness constant but allowing hue and saturation to vary](random-is-bad.pdf){#fig:random-is-bad}

This illustrates both issues we originally raised with random sampling: the straightforward definitions of generatos may have undesirable properties, and that random sampling is inefficient. Let's look at both in turn.

Firstly, notice that with 1000 samples the density of samples is greater at the centre of the circle than the edges. This is a flaw with our definition of the generating distribution. Chosing angle (hue) and radius (saturation) both uniformly at random does not give a uniform distribution over a circle because the area of the circle is more concentrated to the outside of the circle. To correctly sample uniformly over the circle we should bias to larger values of the radius; to be precise, if we transform that uniform distribution by the square root we get the desired output. This is well known in certain circles (pun very much intended) but we should not expect the average programmer to be aware of these details.

Looking at the picture of 100 samples notice how the samples are clumped together, both because of the issue with the definition of the generator discussed above and also just because of the nature of random samples. This clumping is also evident in the picture showing 1000 samples. Without some prior knowledge of where in the sample space bugs are found we should aim to sample
with uniform density across the space.

This simple example illustrates the issues with random sampling. Most programmer deal with discrete structures, like lists and trees, which do not lend themselves to such simple visualization and analysis. What we desire is a general purpose algorithm that can take a description of a generator and produce high quality output. As our first step to this, in the next section we formalize the problem as one of graph search.


## Formalism


## Generators as Distributions

Generators implicitly define a family of distributions, but we need an explicit definition if we're to perform the updates we need to calculate the posterior and measure Bayesian surprise.

Free generators have a single random choice operator, `Select`, for which a reasonable prior is the uniform Dirichlet over the choices given to `Select`. This, taken together with the pure computation that computes the rest of the generator, defines the prior over the generator.

There is a problem, which is the same problem that makes any static analysis of monads difficult: the functions passed to `bind` can make choices at runtime and we may not be able to statically determine what those possible choices are. (If we're prepared to work in the more restrictive setting of selective or applicative functors, then we have no issue.) We can think of a generator as defining a (possibly infinite) tree of choice sequences. If we've never visited a particular part of the tree, any `Selects` within that part will have the prior distribution. We can distinguish other, observed, parts of the tree by the choice sequence that leads to them. Thus we can model the distribution over generators as a map from choice sequences to Dirichlet distributions.


## Calculating Bayesian Surprise

We can use a Monte Carlo scheme to approximate Bayesian surprise. All we have to do it sample distributions from the posteriors, calculate Bayesian surprise for the data $D$ on these sampled distributions, and average over all the samples. This should be sufficient for at least initial results.

The structure of our generator is called a **belief network** or **Bayesian network** and inference in a belief network is NP-hard. Belief networks are in general DAGs, while generators are trees, so there may be a special purpose algorithm we can use. I don't know of one and a quick search didn't find one.


### Algorithms for Maximising Bayesian Surprise

Now let's turn to algorithms that generate data that maximises Bayesian surprise. This is in general intractable, because it reduces to a search over the possibly infinite space of all possible data a generator can generate, or the equivalent search over the possibly infinite tree of choice sequences. We can instead imple ment an approximation. Below are two very simple greedy algorithms. This general problem feels very similar to the explore / exploit problem in bandit algorithms and reinforcement learning, so using ideas from there may be useful.


### $\epsilon$-Greedy Algorithm

The general idea is that, when encountering a `Select` we should choose the least likely option with probability $\epsilon$ and otherwise choose another option. There are many possible definitions for &ldquo;choose another option&rdquo;. A simple example is to choose with uniform probability over all the remaining options.


<a id="org0906c9d"></a>

## Inverse Probability Greedy Algorithm

When encountering a `Select`, sample a distribution from the prior, and then construct a new distribution with probabilities proportional to the **inverse** of the probabilities in the sample. So if we sample the distribution `(0.2, 0.8)` (for a `Select` with two choices) we first calculate `(1/0.2, 1/0.8) = (5, 1.25)`, and then normalize to get `(0.8 0.2)`. We then sample a choice from this new distribution.

\bibliographystyle{kluwer}
\bibliography{/home/noel/Dropbox/Personal/liminal/references}

