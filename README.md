# PrettyPaint
PrettyPaint is for drawing pretty polygons in <a href="https://libgdx.badlogicgames.com/">libGDX</a>. Click [here](EXAMPLES.md) for some examples.


![Screenshot of some PrettyPaint polygons.](https://s3.eu-central-1.amazonaws.com/prettypaint/PrettyPaintScreenshot.jpg "PrettyPaint polygons")

# Description
PrettyPaint by Andreas Modahl is a small library for drawing polygons with texture filling and anti-aliased outlines. It is supposed to be easy and quick 
to set up and use. 

<b>Here are some of the things it can do:</b>


* Merge the outlines and align the textures of overlapping polygons.
* Simple shadows or glow(just wider and less opaque outlines drawn behind)
* Frustum culling
* Draw background(s)

![Unmerged and unaligned. ](https://s3.eu-central-1.amazonaws.com/prettypaint/default.png "Unmerged and unaligned")
![Merged and aligned.](https://s3.eu-central-1.amazonaws.com/prettypaint/merged+and+aligned.png "Merged and aligned")




It depends on two great libraries: 
<a href="http://code.google.com/p/poly2tri/">poly2tri</a> and <a href="http://www.angusj.com/delphi/clipper.php">Clipper</a>. 
I have modified these libraries a little bit to make them work with GWT.

# How to use
For now you must copy the source code from git if you want to try it.
Look at the [demo page](TESTAPPS.md) for examples.


# License
* PrettyPaint:  <a href="https://opensource.org/licenses/MIT">MIT license</a>
* poly2tri: <a href="http://opensource.org/licenses/BSD-3-Clause">New BSD License</a>
* Clipper: <a href="http://www.boost.org/LICENSE_1_0.txt">Boost Software License</a>

# Things im not happy about
* Very sharp angles when combined with thick edges looks bad
* Its not very fast
