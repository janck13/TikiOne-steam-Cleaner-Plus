由于作者已经停止更新，经作者同意，本人接手了开发.

#Tikione Steam Cleaner Plus
## Download installer

TikiOne Steam Cleaner installer is hosted on [GitHub releases](https://github.com/jonathanlermitage/tikione-steam-cleaner/releases).

**Information about Sourceforge**: up to version 2.4.6, TikiOne Steam Cleaner was hosted on SourceForge. I have decided to leave this website to prefer GitHub. Starting from next version, I'll use GitHub only.

## Build, test and package

TikiOne Steam Cleaner is built with [NetBeans](http://netbeans.org) and the latest version of Oracle JDK8.

To build the project:

* load the project with NetBeans and a Java 8 compatible JDK (I use the latest version of NetBeans and Oracle JDK8)
* dependencies should be automatically loaded from the ``./dependencies/`` folder.
* set the working directory to the "dist2" folder. It contains additional configuration files used by base application.

You can now build and run the project.

To package the application, simply merge the "dist" and "dist2" folders.

To bundle a JVM (version 8 or better), copy it into a "jre" folder in the "dist2" folder and launch the NSIS script: it will package TikiOne steam Cleaner with the provided JVM into an EXE installer based on NSIS-Unicode (Nullsoft Scriptable Install System, Unicode version: I use version 2.46-5 from [Google Code](http://code.google.com/p/unsis/downloads/list)).

## Author
* Jonathan Lermitage (<jonathan.lermitage@gmail.com>)
* wbsdty331 (<wbsdty331@gmail.com>)

## Contact

You can ask questions, share ideas or insult me by email (<jonathan.lermitage@gmail.com>) or discuss via [Twitter](https://twitter.com/JLermitage).

中文问题请发送到(<wbsdty331@gmail.com>)，或者[Twitter](https://twitter.com/wbsdty331).

## Contributors
* Dmitry Bolotov (Дмитрий Болотов): Russian and Ukrainian translations
* Boris Klein: German translation
* Ulli Kunz: German translation
* Hauwertlhaufn: German translation
* Zsolt Brechler: Hungarian translation
* Piotr Swat: Polish translation
* Pedro Henrique Viegas Diniz: Portuguese translation
* "ZoSH": Spanish translation
* wbsdty331 : Simplified Chinese translation
* Petr Kudlička: redist detection improvements
* Brian Huqueriza: redist detection improvements
* Antti Mieskolainen: redist detection improvements, GOG and Origin support
* Members of the [CanardPC forum](http://forum.canardpc.com), for their support and cheerfulness

## History

I'm working on this software since Janurary 2012. Here is the full [changelog](https://github.com/jonathanlermitage/tikione-steam-cleaner/blob/master/CHANGELOG.md).

## License

LGPL License

## Donations

You can help me with a PayPal donation ([TikiOne, jonathan@lermitage.biz](http://sourceforge.net/p/tikione/donate/)).