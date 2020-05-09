-dontobfuscate
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
	public static void checkExpressionValueIsNotNull(...);
	public static void checkNotNullExpressionValue(...);
	public static void checkReturnedValueIsNotNull(...);
	public static void checkFieldIsNotNull(...);
	public static void checkParameterIsNotNull(...);
}
-keep class org.koitharu.kotatsu.core.db.entity.* { *; }
-keepclassmembers public class * extends org.koitharu.kotatsu.core.parser.MangaRepository {
	public <init>(...);
}
-dontwarn okhttp3.internal.platform.ConscryptPlatform