# Diversity in Generator Output

## Introduction

Unit testing is the dominant approach to testing in the software industry. Unit testing requires the programmer explicitly defines inputs and expected outputs for the function under test. Therefore, the effectiveness of unit testing is limited by the amount of effort the programmer expends in creating distinct input and output pairs. 

Property based testing, or generative testing, is a lightweight extension of unit testing that aims to improve on unit testing. Instead of the programmer explicitly specifying input and output pairs, the programmer defines generators and properties. A **generator** is a program for generating test inputs, and **properties** are conditions that must hold for the output. By using the computer to generate the input, many more inputs can be generated than when the programmer specifies them by hand and inputs that the programmer may have overlooked can be created.

Property based testing has increasing adoption within industry, with libraries available in many popular languages. However, this doesn't mean existing practice cannot be improved. Our goal here is to make such an improvement by focusing on how generators create the test inputs. 

Property based testing libraries provides an API to construct generators. The standard approach is an implementation of the probability monad, which is simple to implement and to use. The generators so constructed can be viewed as functions from a source of randomness to an output of the desired type, which is the input to the function under test.

Ideally, the output of a generator should be highly **diverse**. Intuitively we want the generated outputs to be as different from each other as possible, to maximize the chances of finding bugs. (We'll define this more formally later.) Random generation is not the best choice for achieving this goal for two reasons: 

1.  A generator defines a distribution over test inputs. Most programmers are rarely statisticians. In practice little attention is paid to the properties of this distribution and generators often have undesirable properties.

2.  Random generation is an inefficient way to explore the space of test inputs. Randomly generated data often displays ``clumping'': data points that are near to one another, or even exactly the same. 

Our focus here is on the second point, though we'll briefly touch on the first as well.

In this paper we look at the task of generating diverse inputs in property based testing. We start by illustrating the problem in practice. When then formalize the setting, and develop a toolbox for constructing algorithms that generate input. We then discuss algorithms to increase diversity, and test these algorithms on several real-world problems.


## The Problem of Diversity in Test Input Generation

Let's start with an example of a very simple property based test, taken from the Doodle library. Doodle, written in Scala, is a library for two-dimensional graphics. Color is a core abstraction in Doodle, and colors can be specified in two equivalent ways:

-   as a triple of red, green, and blue values (RGB); or
-   as a triple of hue, saturation, and lightness (HSL).

The HSL representation is easier for humans to work with, while RGB is what the computer uses to display color. Conversion between the two formats is therefore necessary, but this is not straightforward. The RGB representation defines a cube, with each component an unsigned byte. The HSL representation defines a cylinder, as hue is represented as an angle while saturation and lightness are both floating point numbers in the range 0.0 to 1.0. Therefore, the conversion requires a relatively involved algorithm.

There is a simple property that checks the conversion: converting an HSL color to RGB and back should yield, approximately, the same color. In other words, the conversion from HSL to RGB should be invertible. We can also check the conversion going the other way: from RGB to HSL to RGB. Below is a test for both these properties, encoded in the ScalaCheck property-testing library.

```scala
object ColorSpec extends Properties("Color properties") {
  import doodle.arbitrary._
  import Color._

  property(".toRGBA andThen .toHSLA is the identity") =
    forAll { (hsla: HSLA) =>
      (hsla ~= (hsla.toRGBA.toHSLA))
    }

  property(".toHSLA andThen .toRGBA is the identity") = 
    forAll { (rgba: RGBA) =>
      (rgba ~= (rgba.toHSLA.toRGBA))
    }
}
```

The `~=` operator denotes approximate equality.

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

There is a problem with this generator. [@Fig:random-is-bad] shows 100 and 1000 colors respectively, sampled as described above except that lightness is fixed to 0.7. Fixing lightness makes no substantive change to the points we wish to illustrate, but the two-dimensional visualization is easier to interpret.

![100 and 1000 random colors, keeping lightness constant but allowing hue and saturation to vary](random-is-bad.pdf){#fig:random-is-bad}

This illustrates both issues we originally raised with random sampling: the straightforward definitions of generators may have undesirable properties, and that random sampling is inefficient. Let's look at both in turn.

Firstly, notice that with 1000 samples the density of samples is greater at the centre of the circle than the edges. This is a flaw with our definition of the generating distribution. Chosing angle (hue) and radius (saturation) both uniformly at random does not give a uniform distribution over a circle because the area of the circle is more concentrated to the outside of the circle. To correctly sample uniformly over the circle we should bias to larger values of the radius; to be precise, if we transform that uniform distribution by the square root we get the desired output. This is well known in certain circles (pun very much intended) but we should not expect the average programmer to be aware of this.

Looking at the picture of 100 samples notice how the samples are clumped together, both because of the issue with the definition of the generator discussed above and also just because of the nature of random samples. This clumping is also evident in the picture showing 1000 samples. Without some prior knowledge of where in the sample space bugs are found we should aim to sample
with uniform density across the space of test inputs.

This simple example illustrates the issues with random sampling. Most programmer deal with discrete structures, like lists and trees, which do not lend themselves to such simple visualization and analysis. What we desire is a general purpose algorithm that can take a description of a generator and produce high quality output. As our first step to this, in the next section we formalize the problem of test input generation as one of graph search, and introduce a toolbox of methods for constructing graph search algorithms.


\bibliographystyle{kluwer}
\bibliography{/home/noel/Dropbox/Personal/liminal/references}

