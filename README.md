# PrettyPaint
PrettyPaint is for drawing pretty 2d polygons in <a href="https://libgdx.badlogicgames.com/">libGDX</a>.

![Screenshot of some PrettyPaint polygons.](https://s3.eu-central-1.amazonaws.com/prettypaint/PrettyPaintScreenshot.jpg "PrettyPaint polygons")

# Description
PrettyPaint is a small library for drawing polygons with texture filling and anti-aliased outlines. It is supposed to be easy and quick 
to set up and use. 

<b>Here are some of the things it can do:</b>


* Merge the outlines and align the textures of overlapping polygons.
* Simple shadows or glow(just wider and less opaque outlines drawn behind)
* Frustum culling
* Draw background(s)    (super messy source code)

![Unmerged and unaligned. ](https://s3.eu-central-1.amazonaws.com/prettypaint/default.png "Unmerged and unaligned")
![Merged and aligned.](https://s3.eu-central-1.amazonaws.com/prettypaint/merged+and+aligned.png "Merged and aligned")




It depends on two great libraries: 
<a href="http://code.google.com/p/poly2tri/">poly2tri</a> and <a href="http://www.angusj.com/delphi/clipper.php">Clipper</a>. 
I have modified these libraries a little bit to make them work with GWT.

# How to
For now you must copy the source code from git if you want to try it.

Here is a demo: [source](core/src/org/prettypaint/test/PrettyPaintDemo.java)
[compiled](http://amsoftware.org/PrettyPaintDemo/)


