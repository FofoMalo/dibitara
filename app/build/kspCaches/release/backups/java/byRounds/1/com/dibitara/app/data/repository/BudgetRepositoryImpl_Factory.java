package com.dibitara.app.data.repository;

import com.dibitara.app.data.local.dao.BudgetDao;
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
public final class BudgetRepositoryImpl_Factory implements Factory<BudgetRepositoryImpl> {
  private final Provider<BudgetDao> daoProvider;

  public BudgetRepositoryImpl_Factory(Provider<BudgetDao> daoProvider) {
    this.daoProvider = daoProvider;
  }

  @Override
  public BudgetRepositoryImpl get() {
    return newInstance(daoProvider.get());
  }

  public static BudgetRepositoryImpl_Factory create(Provider<BudgetDao> daoProvider) {
    return new BudgetRepositoryImpl_Factory(daoProvider);
  }

  public static BudgetRepositoryImpl newInstance(BudgetDao dao) {
    return new BudgetRepositoryImpl(dao);
  }
}
