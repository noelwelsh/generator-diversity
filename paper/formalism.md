# Test Input Generation as Graph Search

We're now ready to formalize our setting. We start by formalizing the testing setting, and then turn to generators.

The function under test is an arbitrary function $f: A \rightarrow B$. Our only requirement is that $f$ is a pure function; in other words, it is deterministic. (If this is not the case the whole testing enterprise is pointless.)

We have a generator $g: Random \rightarrow A$, where $Random$ is some source of randomness. Our generators follow the structure in "Parsing Randomness".

Finally we have some post-condition $post: B \rightarrow Bool$, that determines if $f$'s output is acceptable, given some choice of input.

Generators define a directed graph, or more correctly a tree, of possible program executions. Vertices in the graph are calls to `Select`, and edges are the deterministic execution that occurs between one call to `Select` and another. This allows us to formulate the data generation problem as one of graph search.

*This needs more exposition*

This formulation allows us to immediately formulate three baseline algorithm for data generation:

1. random sampling, which is the current practice and which we have discussed earlier;
2. breadth-first search to enumerate all possible test inputs; and
3. depth-first search, also to enumerate all possible test inputs.

In general the size of the set of test inputs is infinite, so complete enumeration is not feasible.

One way to measure diversity is to use Bayesian surprise. Given data $D$, in this case samples from a generator, and a model $M$, in this case a generator, Bayesian surprise is defined as

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
