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
public final class GetMonthlyTransactionsUseCase_Factory implements Factory<GetMonthlyTransactionsUseCase> {
  private final Provider<TransactionRepository> transactionRepositoryProvider;

  public GetMonthlyTransactionsUseCase_Factory(
      Provider<TransactionRepository> transactionRepositoryProvider) {
    this.transactionRepositoryProvider = transactionRepositoryProvider;
  }

  @Override
  public GetMonthlyTransactionsUseCase get() {
    return newInstance(transactionRepositoryProvider.get());
  }

  public static GetMonthlyTransactionsUseCase_Factory create(
      Provider<TransactionRepository> transactionRepositoryProvider) {
    return new GetMonthlyTransactionsUseCase_Factory(transactionRepositoryProvider);
  }

  public static GetMonthlyTransactionsUseCase newInstance(
      TransactionRepository transactionRepository) {
    return new GetMonthlyTransactionsUseCase(transactionRepository);
  }
}
