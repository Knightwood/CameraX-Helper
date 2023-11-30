# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
##########################基本规则###############################
# 1.不能混淆的
    # 被反射的元素,实体类,四大组件,jni调用的java方法
# 1.1 不建议混淆的
    # 自定义控件,js调用的java方法,java的native方法等.

    #Java的反射，为什么不能混淆呢？因为代码混淆，类名、方法名、属性名都改变了，而反射它还是按照原来的名字去反射，结果只射出一个程序崩溃
    #注解用了反射，所以不能混淆。 不混淆任何包含native方法的类的类名以及native方法名，否则找不到本地方法。
    #四大组件不能混淆，因为AndroidManifest.xml文件中是完整的名字,其他应用程序访问组件时可能会用到类的包名加类名，如果经过混淆，可能会无法找到对应组件或者产生异常。
    #自定义view也是带了包名写在xml布局中，不能混淆

# 2.修饰符：
    #libraryjars  声明lib jar文件
    #dontwarn 不提示警告 dontwarn是一个和keep可以说是形影不离,尤其是处理引入的library时.
    #引入的library可能存在一些无法找到的引用和其他问题,在build时可能会发出警告,
    #如果我们不进行处理,通常会导致build中止.
    #因此为了保证build继续,我们需要使用dontwarn处理这些我们无法解决的library的警告.
    #dontnote:指定不去输出打印该类产生的错误或遗漏
# 3.混淆输出结果
    #混淆构建完成之后，会在 <module-name>/build/outputs/mapping/release/ 目录下生成以下文件：

    # dump.txt
    # 说明 APK 内所有类文件的内部结构。

    # mapping.txt
    # 提供混淆前后的内容对照表，内容主要包含类、方法和类的成员变量。

    # seeds.txt
    # 罗列出未进行混淆处理的类和成员。

    # usage.txt
    # 罗列出从 APK 中移除的代码。
# 4.retrace 脚本工具
#   位于Android SDK 路径的 /tools/proguard/bin.
#   结合上文提到的 mapping.txt 文件，就可以将混淆后的崩溃堆栈追踪信息还原成正常情况下的 StackTrace 信息

#   参考: https://jebware.com/blog/?p=418
#   默认情况,不使用任何keep相关指令: 既会压缩(例如，去除无用代码)，也会混淆(例如，重命名事物)类和类成员

    #keep             不压缩，不混淆；即不用于类，也不用于成员

    #keepnames        压缩类及成员，但不混淆它们。也就是说，未使用的代码将被移除。剩下的代码则维持原状。

    #keepclassmembers 只保护类的成员不被压缩和混淆。也就是说，如果类未被使用，则删除类。
#                     如果类被使用，保留并重命名该类。类里的成员维持不变，仍然是之前的名字。

    #keepclassmembernames 只保留类中的成员，防止被混淆，成员没有引用会被移除
#                         这是最宽容的keep指令；它允许ProGuard完成几乎所有工作。
#                         移除未使用的类，剩下的类被重命名，类中未使用的成员将被移除，剩余的成员保留原来的名称。

    #keepclasseswithmembers 保留类和类中的成员，防止被混淆或移除，保留指明的成员
#                           与-keep作用一致。区别是它只适用于拥有类规范中所有成员的类。

    #keepclasseswithmembernames 保留类和类中的成员，防止被混淆，保留指明的成员，成员没有引用会被移除
#                               这条规则与-keepnames一致。区别也是它只适用于拥有类规范中所有成员的类。

#通配符

  # *号： 匹配任意长度字符，但不包含分隔符“.”。比如我们的完整类名是 com.kiylx.test.MyActivity
    #使用com.* ,或是com.kiylx.* 都是无法匹配的，因为* 无法匹配包名中的分隔符，正确的匹配方式是:
    #com.kiylx.*.* ,或者com.kiylx.test.* 。如果不写其他内容，只有一个*，那就表示匹配所有东西
  # **号： 匹配任意长度字符，并且包含包名分隔符"." ,比如-dontwarn android.support.** 就可以匹配
    #android.support包下的所有内容，包括任意长度的子包
  #示例： *号和**号：
    #-keep class cn.hadcn.test.**
    #-keep class cn.hadcn.test.*
    #一颗星表示只是保持该包下的类名，而子包下的类名还是会被混淆；两颗星表示把本包和所含子包下的类名都保持；
    #用以上方法保持类后，你会发现类名虽然未混淆，但里面的具体方法和变量命名还是变了，这时如果既想保持类名，又想保持里面的内容不被混淆，我们就需要以下方法了：
    #-keep class cn.hadcn.test.* {*;}
  #***号：
    #匹配任意参数类型。比如void set*(***)就能匹配人已传入的参数类型，***get*()就能匹配任意返回值的类型

##########################基本规则###############################


#############################################
#
# 对于一些基本指令的添加
#
#############################################
# 代码混淆压缩比，在0~7之间，默认为5，一般不做修改
-optimizationpasses 5

# 混合时不使用大小写混合，混合后的类名为小写
-dontusemixedcaseclassnames

# 混淆时不记录日志
-verbose

# 指定不去忽略非公共库的类
-dontskipnonpubliclibraryclasses

# 指定不去忽略非公共库的类成员
-dontskipnonpubliclibraryclassmembers

# 不做预校验，preverify是proguard的四个步骤之一，Android不需要preverify，去掉这一步能够加快混淆速度。
-dontpreverify

# 忽略警告
#-ignorewarning

# 优化不优化输入的类文件
-dontoptimize

# 抛出异常时保留代码行号 可以提升我们的 StackSource 查找效率
-keepattributes SourceFile,LineNumberTable

# 指定混淆是采用的算法，后面的参数是一个过滤器
# 这个过滤器是谷歌推荐的算法，一般不做更改
-optimizations !code/simplification/cast,!field/*,!class/merging/*


#############################################
#
# Android开发中一些需要保留的公共部分
#
#############################################
#避免混淆注解类
#-dontwarn android.annotation
-keepattributes *Annotation*

# 避免混淆泛型
-keepattributes Signature

#避免混淆内部类
-keepattributes InnerClasses
# 保留我们使用的四大组件，自定义的Application等等这些类不被混淆
# 因为这些子类都有可能被外部调用
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Fragment
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View
#-keep public class com.android.vending.licensing.ILicensingService

#androidx包使用混淆
-keepnames class com.google.android.material.** {*;}
-keepnames class androidx.** {*;}
-keepnames public class * extends androidx.**
-keepnames interface androidx.** {*;}
-dontwarn com.google.android.material.**
-dontnote com.google.android.material.**
-dontwarn androidx.**

# 保留support下的所有类及其内部类
-keep class android.support.** {*;}

# 保留继承的 support库
#-keep public class * extends android.support.v4.**
#-keep public class * extends android.support.v7.**
#-keep public class * extends android.support.annotation.**

# 保留R 资源id不被混淆 不使用反射不需要keep
-keep class **.R$* {*;}

# 保留本地native方法不被混淆
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保留在Activity中的方法参数是view的方法，
# 这样以来我们在layout中写的onClick就不会被影响
-keepclassmembers class * extends android.app.Activity{
    public void *(android.view.View);
}

# 保留枚举类不被混淆
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 避免混淆序列化类
# 不混淆Parcelable和它的实现子类，还有Creator成员变量
-keep class * implements android.os.Parcelable {
        public static final android.os.Parcelable$Creator *;
}

# 不混淆Serializable和它的实现子类、其成员变量
-keep public class * implements java.io.Serializable {*;}
-keepclassmembers class * implements java.io.Serializable {
        static final long serialVersionUID;
        private static final java.io.ObjectStreamField[] serialPersistentFields;
        private void writeObject(java.io.ObjectOutputStream);
        private void readObject(java.io.ObjectInputStream);
        java.lang.Object writeReplace();
        java.lang.Object readResolve();
}

# 保留我们自定义控件（继承自View）不被混淆
-keep public class * extends android.view.View{
    *** get*();
    void set*(***);
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
-keepclasseswithmembers class * {
        public <init>(android.content.Context, android.util.AttributeSet);
        public <init>(android.content.Context, android.util.AttributeSet, int);
}

# webview 混淆
# Webview 相关不混淆
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
#-keepclassmembers class * extends android.webkit.WebViewClient {
#        public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
#        public boolean *(android.webkit.WebView, java.lang.String);
#}
#-keepclassmembers class * extends android.webkit.WebViewClient {
#        public void *(android.webkit.WebView, java.lang.String);
# }

# 使用GSON、fastjson等框架时，所写的JSON对象类不混淆，否则无法将JSON解析成对应的对象
#-keepclassmembers class * {
#         public <init>(org.json.JSONObject);
#}

####################################################
#
#              其他
#
####################################################

#禁用日志打印
-assumenosideeffects class android.util.Log {
  public static *** v(...);
  public static *** d(...);
  public static *** i(...);
  public static *** w(...);
  public static *** e(...);
}

#kotlin serialization 如果要序列化具有命名伴随对象的类，详情: https://github.com/Kotlin/kotlinx.serialization#android
##############################################kotlin serialization######################################
## Keep `Companion` object fields of serializable classes.
## This avoids serializer lookup through `getDeclaredClasses` as done for named companion objects.
#-if @kotlinx.serialization.Serializable class **
#-keepclassmembers class <1> {
#    static <1>$Companion Companion;
#}
#
## Keep `serializer()` on companion objects (both default and named) of serializable classes.
#-if @kotlinx.serialization.Serializable class ** {
#    static **$* *;
#}
#-keepclassmembers class <2>$<3> {
#    kotlinx.serialization.KSerializer serializer(...);
#}
#
## Keep `INSTANCE.serializer()` of serializable objects.
#-if @kotlinx.serialization.Serializable class ** {
#    public static ** INSTANCE;
#}
#-keepclassmembers class <1> {
#    public static <1> INSTANCE;
#    kotlinx.serialization.KSerializer serializer(...);
#}

# @Serializable and @Polymorphic are used at runtime for polymorphic serialization.
#-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Don't print notes about potential mistakes or omissions in the configuration for kotlinx-serialization classes
# See also https://github.com/Kotlin/kotlinx.serialization/issues/1900
#-dontnote kotlinx.serialization.**

# Serialization core uses `java.lang.ClassValue` for caching inside these specified classes.
# If there is no `java.lang.ClassValue` (for example, in Android), then R8/ProGuard will print a warning.
# However, since in this case they will not be used, we can disable these warnings
#-dontwarn kotlinx.serialization.internal.ClassValueReferences

#############################################kotlin serialization######################################

##-okhttp3
#-dontwarn com.squareup.okhttp.**
#-keep class com.squareup.okhttp.**{*;}
#-dontwarn okio.**
# -keep public class org.codehaus.* { *; }
# -keep public class java.nio.* { *; }
#
## Retrofit
#-keep class retrofit2.** { *; }
#-dontwarn retrofit2.**
#-keepattributes Exceptions
#-dontwarn javax.annotation.**
#
##保留json转换
# -keep class com.jakewharton.retrofit2.converter.kotlinx.serialization.**