package com.dibitara.app.domain.usecase;

import com.dibitara.app.domain.repository.TransactionRepository;
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
public final class AddTransactionUseCase_Factory implements Factory<AddTransactionUseCase> {
  private final Provider<TransactionRepository> transactionRepositoryProvider;

  public AddTransactionUseCase_Factory(
      Provider<TransactionRepository> transactionRepositoryProvider) {
    this.transactionRepositoryProvider = transactionRepositoryProvider;
  }

  @Override
  public AddTransactionUseCase get() {
    return newInstance(transactionRepositoryProvider.get());
  }

  public static AddTransactionUseCase_Factory create(
      Provider<TransactionRepository> transactionRepositoryProvider) {
    return new AddTransactionUseCase_Factory(transactionRepositoryProvider);
  }

  public static AddTransactionUseCase newInstance(TransactionRepository transactionRepository) {
    return new AddTransactionUseCase(transactionRepository);
  }
}
