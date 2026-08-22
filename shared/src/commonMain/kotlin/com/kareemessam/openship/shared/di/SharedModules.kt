package com.kareemessam.openship.shared.di

import com.kareemessam.openship.shared.client.DeployLogsRepository
import com.kareemessam.openship.shared.client.DiscoveryService
import com.kareemessam.openship.shared.client.HttpClientFactory
import com.kareemessam.openship.shared.client.MonitorRepository
import com.kareemessam.openship.shared.client.ProjectsRepository
import com.kareemessam.openship.shared.viewmodel.ConnectViewModel
import com.kareemessam.openship.shared.viewmodel.DeployLogsViewModel
import com.kareemessam.openship.shared.viewmodel.MonitorViewModel
import com.kareemessam.openship.shared.viewmodel.ProjectsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val sharedModule = module {
    single { HttpClientFactory.create() }
    single { DiscoveryService(get()) }
    single { ProjectsRepository(get(), get()) }
    single { DeployLogsRepository(get(), get()) }
    single { MonitorRepository(get(), get()) }
    viewModel { ConnectViewModel(get(), get()) }
    viewModel { ProjectsViewModel(get(), get()) }
    viewModel { DeployLogsViewModel(get(), get()) }
    viewModel { MonitorViewModel(get(), get()) }
}
