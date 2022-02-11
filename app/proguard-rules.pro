-optimizationpasses 8
-dontobfuscate
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
	public static void checkExpressionValueIsNotNull(...);
	public static void checkNotNullExpressionValue(...);
	public static void checkReturnedValueIsNotNull(...);
	public static void checkFieldIsNotNull(...);
	public static void checkParameterIsNotNull(...);
	public static void checkNotNullParameter(...);
}
-keep public class ** extends org.koitharu.kotatsu.base.ui.BaseFragment
-keep class org.koitharu.kotatsu.core.db.entity.* { *; }
-dontwarn okhttp3.internal.platform.ConscryptPlatform