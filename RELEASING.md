Releasing
========

This repository is set up for autorelease. The process involves pushing a new git tag.

 1. Increase the SNAPSHOT version in `gradle.properties`. (Note: `-SNAPSHOT` is automatically removed if it is production release)
 1. Update the `CHANGELOG.md` for the impending release.
 1. `git commit -am "Prepare for release X.Y.Z."` (where X.Y.Z is the new version)
 1. `git tag -a X.Y.Z -m "Version X.Y.Z"` (where X.Y.Z is the new version)
 1. `git push && git push --tags`
