analytics-android-integration-comscore
======================================

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.segment.analytics.android.integrations/comscore/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.segment.analytics.android.integrations/comscore)
[![Javadocs](http://javadoc-badge.appspot.com/com.segment.analytics.android.integrations/comscore.svg?label=javadoc)](http://javadoc-badge.appspot.com/com.segment.analytics.android.integrations/comscore)

ComScore integration for [analytics-android](https://github.com/segmentio/analytics-android).


## Installation

To install the Segment-ComScore integration, first include mavenCentral as a source repository in your gradle file:

```
allprojects {
  repositories {
    mavenCentral()
  }
}
```

Then include the Segment-Comscore integration as a dependency, in the dependencies section of your gradle file:

```
implementation 'com.segment.analytics.android.integrations:comscore:+'
```


## Usage

After adding the dependency, you must register the integration with our SDK.  To do this, import the ComScore integration:


```
import com.segment.analytics.android.integrations.comscore.ComScoreIntegration;

```

And add the following line:

```
 analytics = new Analytics.Builder(this, "write_key")
                .use(ComScoreIntegration.FACTORY)
                .build();
```

Please see [our documentation](https://segment.com/docs/integrations/comscore/#mobile) for more information.

## License

```
WWWWWW||WWWWWW
 W W W||W W W
      ||
    ( OO )__________
     /  |           \
    /o o|    MIT     \
    \___/||_||__||_|| *
         || ||  || ||
        _||_|| _||_||
       (__|__|(__|__|

The MIT License (MIT)

Copyright (c) 2014 Segment, Inc.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
