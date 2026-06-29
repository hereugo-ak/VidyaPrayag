package com.littlebridge.enrollplus.ui.v2.screens.school

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.littlebridge.enrollplus.feature.idcard.presentation.IdCardViewModel
import com.littlebridge.enrollplus.ui.v2.components.VCard
import com.littlebridge.enrollplus.ui.v2.components.VTopTabs
import com.littlebridge.enrollplus.ui.v2.screens.collectAsStateV2
import com.littlebridge.enrollplus.ui.v2.theme.VTheme
import org.koin.compose.viewmodel.koinViewModel

private enum class IdCardTab(val label: String) {
    Templates("Templates"),
    Generate("Generate"),
    Cards("Cards"),
}

@Composable
fun IdCardScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: IdCardViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateV2()
    var activeTab by remember { mutableStateOf(IdCardTab.Templates) }
    val templatesScrollState = rememberScrollState()
    var scrollToBuilder by remember { mutableStateOf(false) }

    LaunchedEffect(scrollToBuilder) {
        if (scrollToBuilder) {
            activeTab = IdCardTab.Templates
            templatesScrollState.animateScrollTo(templatesScrollState.maxValue)
            scrollToBuilder = false
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadTemplates()
        viewModel.loadCards()
    }

    VTheme {
        Column(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .background(VTheme.colors.background),
        ) {
            com.littlebridge.enrollplus.ui.v2.components.VBackHeader(title = "ID Cards", onBack = onBack)

            IdCardStatsBanner(
                totalCards = state.cards.size,
                studentCards = state.cards.count { it.personType == "student" },
                teacherCards = state.cards.count { it.personType == "teacher" },
                staffCards = state.cards.count { it.personType == "staff" },
                onBadgeClick = { scrollToBuilder = true },
            )

            VTopTabs(
                tabs = IdCardTab.entries.map { it.label },
                selected = activeTab.label,
                onSelect = { label -> activeTab = IdCardTab.entries.first { it.label == label } },
            )

            AnimatedContent(
                targetState = activeTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "idcard-tab",
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) { tab ->
                when (tab) {
                    IdCardTab.Templates -> TemplatesTab(state, viewModel, templatesScrollState)
                    IdCardTab.Generate -> GenerateTab(state, viewModel)
                    IdCardTab.Cards -> CardsTab(state, viewModel)
                }
            }
        }
    }
}
