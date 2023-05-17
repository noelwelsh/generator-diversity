# Bayesian Surprise

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

