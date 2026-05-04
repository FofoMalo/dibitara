package com.dibitara.app.domain.usecase;

import com.dibitara.app.domain.repository.BudgetRepository;
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
public final class GetMonthlyBudgetUseCase_Factory implements Factory<GetMonthlyBudgetUseCase> {
  private final Provider<BudgetRepository> budgetRepositoryProvider;

  public GetMonthlyBudgetUseCase_Factory(Provider<BudgetRepository> budgetRepositoryProvider) {
    this.budgetRepositoryProvider = budgetRepositoryProvider;
  }

  @Override
  public GetMonthlyBudgetUseCase get() {
    return newInstance(budgetRepositoryProvider.get());
  }

  public static GetMonthlyBudgetUseCase_Factory create(
      Provider<BudgetRepository> budgetRepositoryProvider) {
    return new GetMonthlyBudgetUseCase_Factory(budgetRepositoryProvider);
  }

  public static GetMonthlyBudgetUseCase newInstance(BudgetRepository budgetRepository) {
    return new GetMonthlyBudgetUseCase(budgetRepository);
  }
}
