package com.dibitara.app.presentation.dashboard;

import com.dibitara.app.domain.usecase.GetMonthlyBudgetUseCase;
import com.dibitara.app.domain.usecase.GetMonthlyTransactionsUseCase;
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
public final class DashboardViewModel_Factory implements Factory<DashboardViewModel> {
  private final Provider<GetMonthlyBudgetUseCase> getMonthlyBudgetProvider;

  private final Provider<GetMonthlyTransactionsUseCase> getMonthlyTransactionsProvider;

  public DashboardViewModel_Factory(Provider<GetMonthlyBudgetUseCase> getMonthlyBudgetProvider,
      Provider<GetMonthlyTransactionsUseCase> getMonthlyTransactionsProvider) {
    this.getMonthlyBudgetProvider = getMonthlyBudgetProvider;
    this.getMonthlyTransactionsProvider = getMonthlyTransactionsProvider;
  }

  @Override
  public DashboardViewModel get() {
    return newInstance(getMonthlyBudgetProvider.get(), getMonthlyTransactionsProvider.get());
  }

  public static DashboardViewModel_Factory create(
      Provider<GetMonthlyBudgetUseCase> getMonthlyBudgetProvider,
      Provider<GetMonthlyTransactionsUseCase> getMonthlyTransactionsProvider) {
    return new DashboardViewModel_Factory(getMonthlyBudgetProvider, getMonthlyTransactionsProvider);
  }

  public static DashboardViewModel newInstance(GetMonthlyBudgetUseCase getMonthlyBudget,
      GetMonthlyTransactionsUseCase getMonthlyTransactions) {
    return new DashboardViewModel(getMonthlyBudget, getMonthlyTransactions);
  }
}
