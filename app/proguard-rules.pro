# ─── Kotlin ───────────────────────────────────────────────────────────────────
# Conserver les métadonnées Kotlin utilisées par la réflexion (coroutines, sérialisation…)
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-keep class kotlin.Metadata { *; }

# ─── Room ─────────────────────────────────────────────────────────────────────
# Room génère du code à la compilation (KSP) et accède aux entités par réflexion au runtime.
# On protège toutes les classes annotées @Entity et @Dao.
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Database class * { *; }
-keepclassmembers class * {
    @androidx.room.* <fields>;
}

# ─── Hilt / Dagger ────────────────────────────────────────────────────────────
# Hilt intègre ses propres règles consumer ProGuard — rien d'obligatoire ici.
# On conserve quand même les classes @HiltViewModel pour la navigation Compose.
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# ─── DataStore ────────────────────────────────────────────────────────────────
# DataStore Preferences est basé sur Protobuf lite — aucune règle supplémentaire requise.

# ─── Security Crypto (EncryptedSharedPreferences / Tink) ─────────────────────
# Les annotations errorprone de Tink sont compile-time only — elles n'existent pas au runtime.
-keep class androidx.security.crypto.** { *; }
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi

# ─── Vico Charts ──────────────────────────────────────────────────────────────
-keep class com.patrykandpatrick.vico.** { *; }

# ─── Compose ──────────────────────────────────────────────────────────────────
# Le plugin Compose gère ses propres règles via le compilateur.
# On conserve les lambdas pour éviter des crashs sur certains appareils.
-keepclassmembers class * {
    ** Companion;
}

# ─── Application ──────────────────────────────────────────────────────────────
# Conserver toutes les classes du package applicatif (entités, modèles domain)
-keep class com.dibitara.app.** { *; }
