package com.dibitara.app.presentation.auth;

import com.dibitara.app.security.BiometricAuthManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class AuthViewModel_Factory implements Factory<AuthViewModel> {
  private final Provider<BiometricAuthManager> biometricAuthManagerProvider;

  public AuthViewModel_Factory(Provider<BiometricAuthManager> biometricAuthManagerProvider) {
    this.biometricAuthManagerProvider = biometricAuthManagerProvider;
  }

  @Override
  public AuthViewModel get() {
    return newInstance(biometricAuthManagerProvider.get());
  }

  public static AuthViewModel_Factory create(
      Provider<BiometricAuthManager> biometricAuthManagerProvider) {
    return new AuthViewModel_Factory(biometricAuthManagerProvider);
  }

  public static AuthViewModel newInstance(BiometricAuthManager biometricAuthManager) {
    return new AuthViewModel(biometricAuthManager);
  }
}
