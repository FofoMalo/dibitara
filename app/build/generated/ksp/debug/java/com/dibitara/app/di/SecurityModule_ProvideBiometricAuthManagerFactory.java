package com.dibitara.app.di;

import com.dibitara.app.security.BiometricAuthManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class SecurityModule_ProvideBiometricAuthManagerFactory implements Factory<BiometricAuthManager> {
  @Override
  public BiometricAuthManager get() {
    return provideBiometricAuthManager();
  }

  public static SecurityModule_ProvideBiometricAuthManagerFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static BiometricAuthManager provideBiometricAuthManager() {
    return Preconditions.checkNotNullFromProvides(SecurityModule.INSTANCE.provideBiometricAuthManager());
  }

  private static final class InstanceHolder {
    private static final SecurityModule_ProvideBiometricAuthManagerFactory INSTANCE = new SecurityModule_ProvideBiometricAuthManagerFactory();
  }
}
