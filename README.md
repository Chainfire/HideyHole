This is the sauce for the [Hidey Hole](https://play.google.com/store/apps/details?id=eu.chainfire.hideyhole) app.
There's also sauce for the [Django+GKE backend](https://github.com/Chainfire/HideyHole-Backend) available.

[LICENSE](./LICENSE) is GPLv3.

---

Hidey Hole is an app that aggregates wallpapers for the Samsung Galaxy
S10 family of devices (S10/S10e/S10+), primarily those that are made to
obscure the camera cutout (hiding the hole).

It will crash (wont-fix) on any other device!

I have no hand in the wallpapers themselves, those are made/posted by
users in the [/r/S10wallpapers sub on Reddit](https://www.reddit.com/r/S10wallpapers/).
This app has a [Reddit thread](https://www.reddit.com/r/S10wallpapers/comments/b1ydyh/hidey_hole_an_app_for_this_sub/) as well.

The wallpapers are synced from the /r/S10wallpapers sub every hour, and
from [Galaxy S10 Wallpapers](https://www.galaxys10wallpapers.com/) every
six hours.

---

Of course downloading wallpapers manually and setting them as wallpaper
is easy, and do you really need yet another wallpaper app? But I
personally also like to slightly adjust them. For example, I usually
turn down the brightness a bit to make text on the home-/lockscreen
better readable, so that feature is in the app. Other adjustments
include contrast, blackpoint, and saturation.

The hole thing with these devices intrigues me as well. So I wanted to
know if I could determine in code where they were, and auto-realign/scale
images made for one model to another. While the cameras are close to
each-other, they don't overlap exactly, and this discrepancy shows when
using an image made for one device on another (out of sync hole). Works
pretty well on S10/S10E, leaves something to be desired on the S10+.
The algorithm is generic so support for more devices should be easy to
add. (This idea is really what led me to build this, might as well
release it now I've satisfied my curiosity).

As an example of the above, the height of the cutout on the S10+ is a
little bit less than that of the S10. When using an S10+ image on the
S10, you still get a curve of missing pixels below the cutout. This
realign/scale feature slightly zooms and offsets the image, so the
holes align.

### Features

- Browse holey images

- Set homescreen / lockscreen / both wallpapers

- Image adjustments: brightness, contrast, blackpoint, saturation

- Image scaling: align the image's hole to your current device

- Sorting by new or popular

- Device filtering

- Category filtering

- Download wallpapers

### Notes

The scraper takes all images from the sub that have a dedicated post that
directly links to it. Only reddit and imgur links are currently supported.
Minimum width is 640 pixels, and the aspect ratio has to be just right.

I might add scraping the comments at some point, but this is not
currently done.

The category a wallpaper ends up in is decided by the flair of the post.

A lot of recent images include the phone's frame. While those work fine
on the exact device they were meant for, they do not rescale well to
other devices. (And IMHO it doesn't look good, but that's just my
opinion you are free to completely ignore).

### Freedom!

This app is free, without in-app purchases, without ads, without tracking (other than popularity of wallpapers), but *with* GPLv3 [sauce](https://github.com/Chainfire/HideyHole). The backend has [sauce](https://github.com/Chainfire/HideyHole-Backend) too!

### Download

You can grab it from [Google Play](https://play.google.com/store/apps/details?id=eu.chainfire.hideyhole).

[Screenshot#1](https://lh3.googleusercontent.com/_sAn1BBSw7H8zlvGhQMIKUMrlHMCv-mPufxt_GCDokcUouyBNES-_TE_s_OVIP7CONo=w2048-h1063-rw)

[Screenshot#2](https://lh3.googleusercontent.com/dmBBBfWMiubb2XqQErbHDU_vnpMO_q69EtWkumNq89cqXeEnGhI-9VKLZijFs6Uomg=w2048-h1063-rw)

### Feedback

It puts it in the [Reddit thread](https://www.reddit.com/r/S10wallpapers/comments/b1ydyh/hidey_hole_an_app_for_this_sub/) or in the [GitHub issue tracker](https://github.com/Chainfire/HideyHole/issues).

### TODO

You can find the TODO list in the [issue tracker](https://github.com/Chainfire/HideyHole/issues?utf8=%E2%9C%93&q=is%3Aissue).

### Enjoy!
Or not.
