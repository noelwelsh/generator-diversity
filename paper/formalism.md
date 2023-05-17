# Test Input Generation as Graph Search

We're now ready to formalize our setting. We start by formalizing the testing setting, and then turn to generators.

The function under test is an arbitrary function $f: A \rightarrow B$. Our only requirement is that $f$ is a pure function; in other words, it is deterministic. (If this is not the case the whole testing enterprise is pointless.)

A generator is a function $g: Random \rightarrow A$, where $Random$ is some source of randomness. Our generators follow the structure in "Parsing Randomness".

Finally we have some post-condition $post: B \rightarrow Bool$, that determines if $f$'s output is acceptable.

Generators define a directed graph, or more specifically a tree, of possible program executions. Vertices in the graph are calls to `Select`, and edges are the deterministic execution that occurs between one call to `Select` and another. This allows us to formulate the data generation problem as one of graph search. Generating data involves following edges through the graph until they terminate at a node with no further edges: a **sink**. How we choose between available edges is where things get interesting. In the next section we discuss algorithms for graph search.

*This needs more exposition*

Formulating generators as directed graphs immediately suggests classical algorithms for graph search: breadth-first search, depth-first search, and a^\*-search. There is also the current practice of random generation.

We can describe some of the characteristics that differentiate algorithms:

- Deterministic vs random: deterministic algorithms always generate output in the same order, whereas random algorithms do not. Breadth-first and depth-first search are exampels of deterministic algorithms.
- Adaptive vs non-adaptive. Adaptive algorithms take account of information gained in prior runs to attempt to guide the search. We have not yet seen any adaptive algorithms. The No Free Lunch theorem suggests adaptive algorithms cannot out perform non-adaptive algorithms over all possible generators, but we are only concerning ourselves with the relatively small subset of actual generators used in practice. In this situation we might expect an adaptive algorithm to out-perform a non-adaptive one.
- Memory usage. - In general the size of the set of test inputs is infinite, so any algorithm with memory requirements proportional to this size may be problematic. Random sampling, which is the current practice, requires no memory.

We now describe tools we can use to build more complex algorithms.

## Quasi-random Sequences

Quasi-random sequences are deterministic algorithms that generate output with similar properties to random sequences. There are two classes of algorithms of interest to us:

- low discrepancy sequences, such as the Halton and Sobol sequence, which are the quasi-random equivalent of the uniform distribution on $\mathbb{R}^N$; and
- quasi-random walks, such as the rotor-router algorithm, which are the quasi-random equivalent of a random walk on a graph.


## Bayesian Surprise

One way to measure diversity is to use Bayesian surprise. Given data $D$, in this case samples from a generator, and a model $M$, in this case a generator, Bayesian surprise is defined as

\begin{equation}
Surprise(D, M) = KL(P(M | D), P(M))
\end{equation}

$KL$ is the KL-divergence, defined as

\begin{equation}
KL(P(M | D), P(M)) = \int_M P(M | D)  log\big( \frac{P(M|D)}{P(M)} \big)dM
\end{equation}

There are several terms in the equations above that are standard in Bayesian statistics but it is perhaps not clear how they relate to generators. What follows is brief sketch of Bayesian statistics.

Let's assume we have some way of generating data, our generator, which assigns a probability to each possible output. In other words, the generator defines a probability distribution. In keeping with the definitions above, we'll label our generator $M$, and the data $D$. The generator defines $P(D | M)$, the probability of the data given the generator.

In the Bayesian view we don't commit to single generator but instead consider a distribution over generators $P(M)$. This is known as the **prior distribution**. When we see data we can update this distribution to produce $P(M | D)$, the distribution over generators given data, known as the **posterior distribution**. How do we compute the posterior? Bayes theorem tells us that

\begin{equation}
P(M | D) = \frac{P(D|M)P(M)}{P(D)}
\end{equation}

where

\begin{equation}
P(D) = \int_M P(D|M)P(M)
\end{equation}

This tells us in theory how we can compute the posterior, but how does it work in practice? The answer is that it depends on the particular form of the prior. There is a class of prior distributions for which we easily compute the posterior. These are known as **conjugate priors**. If $M$ is choosing between discrete choices (a categorical distribution) and we use the Dirichlet distribution as our prior, the posterior will also be a Dirichlet distribution. There is also a simple rule that relates the posterior to the prior. Hence the Dirichlet is called the conjugate prior for the categorical distribution.

Note the `Select` function in **Parsing Randomness** is a categorical distribution, and thus the Dirichlet is an appropriate prior for it.

We've now defined all the terms in the definition of Bayesian surprise, but it's worth quickly emphasizing the implications for generators. The first implication is that we don't view a generator as defining a particular distribution, but rather defining a family of distributions. This is actually implicit in the definition of free generators, as they do not have probabilities attached to choices. Developers do not pay any particular attention to the distribution their generators define, in my experience, so this also does not conflict with the practice of property based testing. The second implication is that the posterior makes the observed data more likely, and hence less surprising should it be generated again, and therefore the posterior acts as a summary of the data that has been generated to date.


## Markov Decision Processes

A Markov Decision Process (MDP)

In our case the MDP is deterministic, and we are purely exploration.

