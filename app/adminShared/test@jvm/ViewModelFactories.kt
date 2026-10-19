package com.usuallycoding.portfolio.admin

import com.usuallycoding.portfolio.admin.data.AdminRepositoryImpl
import com.usuallycoding.portfolio.admin.data.AdminSession
import com.usuallycoding.portfolio.admin.presentation.LinksViewModel
import com.usuallycoding.portfolio.admin.presentation.ProfileViewModel
import com.usuallycoding.portfolio.admin.presentation.ProjectEditorViewModel
import com.usuallycoding.portfolio.admin.presentation.ProjectsViewModel

/** Builds view models over the real repository, with only the HTTP layer faked. */
fun ProfileViewModel(session: AdminSession) = ProfileViewModel(AdminRepositoryImpl(session))

fun LinksViewModel(session: AdminSession) = LinksViewModel(AdminRepositoryImpl(session))

fun ProjectsViewModel(session: AdminSession) = ProjectsViewModel(AdminRepositoryImpl(session))

fun ProjectEditorViewModel(session: AdminSession, projectId: Long?) = ProjectEditorViewModel(AdminRepositoryImpl(session), projectId)
