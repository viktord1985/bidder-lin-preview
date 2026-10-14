Instructions for newer Macs (Apple Silicon)

(1) get the formula path
brew formula opencv

(2) copy it into this folder
cp /opt/homebrew/opt/opencv/.brew/opencv.rb ./opencv.rb

(3) change the config in opencv.rb

-DBUILD_opencv_java=OFF
Replace it with:
-DBUILD_opencv_java=ON

(4)
brew install --build-from-source ./opencv.rb
or
brew reinstall --build-from-source ./opencv.rb 

(5)
the output (native lib + jar) is located in
/opt/homebrew/Cellar/opencv/4.11.0_1/share/java/opencv4/

---
Instructions for older Macs (x86 build from sources)

the Java build doesn't run without ant, and ant doesn't run without JAVA_HOME

1) install ant and the cmake prerequisites
brew install ant
brew install cmake pkg-config libpng jpeg libtiff openexr

1a) find and set JAVA_HOME for ant
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_211.jdk/Contents/Home
to verify JAVA_HOME run
$JAVA_HOME/bin/java -version

if Java was installed via zulu
readlink -f $(which java)
/Users/mac/.sdkman/candidates/java/17.0.8.fx-zulu/zulu-17.jdk/Contents/Home/bin/java
then JAVA_HOME will be
export JAVA_HOME=/Users/mac/.sdkman/candidates/java/17.0.8.fx-zulu/zulu-17.jdk/Contents/Home
and test again
$JAVA_HOME/bin/java -version

if it fails on dependencies, you can wait for a fix, or take the risk
brew install --ignore-dependencies ant
and then try installing the dependencies from alternative sources

2) clone the sources
git clone https://github.com/opencv/opencv.git

3) 
cd opencv

4) create a build folder to keep the sources clean
mkdir build && cd build

5) this generates the build config and prints a report; check carefully that JAVA (and ant) are enabled
don't forget to set the correct JAVA_HOME at this step

cmake -D CMAKE_BUILD_TYPE=RELEASE \
-D CMAKE_INSTALL_PREFIX=/usr/local \
-D BUILD_opencv_java=ON \
-D BUILD_opencv_python=OFF \
-D WITH_CUDA=OFF \
-D WITH_OPENGL=OFF \
-D WITH_QT=OFF \
-D JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk1.8.0_211.jdk/Contents/Home \
../

6)
make -j8

7) from the build folder, install all libs into the system
sudo make install

// Xcode download
https://developer.apple.com/download/all/
(sign in with your Apple ID)
Note: don't enable the extra security option offered there

--- --- --- --- --- --- --- --- 
Instructions without building (Xcode was installed, the only issue was imath)

copy from opencv_other_libs.zip into the system
opencv_other_libs.zip -> /usr/local/lib/

brew install imath (required, but brew installs the latest while the opencv lib is linked against 3.1.29)
brew install libdeflate 

fixing the imath issue

brew uninstall imath
cd ~/Downloads
git clone https://github.com/AcademySoftwareFoundation/Imath.git
cd Imath
git checkout v3.1.9   # or v3.1.11 — closest to 3.1.29
cmake -B build -DCMAKE_INSTALL_PREFIX=/usr/local/opt/imath
cmake --build build --target install
