package com.paradoxo.amadeus.onboarding.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.paradoxo.amadeus.onboarding.model.OnboardingPage
import com.paradoxo.amadeus.onboarding.ui.components.BottomBanner
import com.paradoxo.amadeus.onboarding.ui.components.DotsIndicator
import com.paradoxo.amadeus.onboarding.ui.components.NavigationRow
import com.paradoxo.amadeus.onboarding.ui.pages.AgentModeContent
import com.paradoxo.amadeus.onboarding.ui.pages.AutoTasksContent
import com.paradoxo.amadeus.onboarding.ui.pages.ControlContent
import com.paradoxo.amadeus.onboarding.ui.pages.GetStartedContent
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    pages: List<OnboardingPage> = OnboardingPage.pages,
    onFinish: (dontShowAgain: Boolean) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    var dontShowAgain by remember { mutableStateOf(false) }

    fun goNext() {
        if (pagerState.currentPage < pages.size - 1) {
            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
        } else {
            onFinish(dontShowAgain)
        }
    }

    fun goPrevious() {
        if (pagerState.currentPage > 0) {
            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = true
            ) { pageIndex ->
                val page = pages[pageIndex]
                PageCard(
                    page = page,
                    currentPage = pagerState.currentPage,
                    totalPages = pages.size,
                    onSkip = { onFinish(dontShowAgain) },
                    onPrevious = ::goPrevious,
                    onNext = ::goNext
                )
            }

            BottomBanner(
                onExplore = { onFinish(dontShowAgain) },
                dontShowAgain = dontShowAgain,
                onDontShowAgainChange = { dontShowAgain = it }
            )
        }
    }
}

@Composable
private fun PageCard(
    page: OnboardingPage,
    currentPage: Int,
    totalPages: Int,
    onSkip: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp)
            .shadow(6.dp, RoundedCornerShape(28.dp))
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top bar: dots + skip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DotsIndicator(
                count = totalPages,
                current = currentPage,
                activeColor = page.accentColor
            )
            TextButton(onClick = onSkip) {
                Text(
                    "Pular",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF888888)
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Illustration
        when (page.contentType) {
            OnboardingPage.ContentType.AGENT_MODE -> AgentModeContent(bgColor = page.bgColor, accentColor = page.accentColor)
            OnboardingPage.ContentType.AUTO_TASKS -> AutoTasksContent(bgColor = page.bgColor, accentColor = page.accentColor)
            OnboardingPage.ContentType.CONTROL -> ControlContent(bgColor = page.bgColor, accentColor = page.accentColor)
            OnboardingPage.ContentType.GET_STARTED -> GetStartedContent(bgColor = page.bgColor, accentColor = page.accentColor)
        }

        Spacer(Modifier.height(8.dp))

        // Title
        Text(
            text = buildAnnotatedString {
                append(page.title)
                if (page.titleSuffix.isNotEmpty()) {
                    withStyle(SpanStyle(color = page.accentColor)) { append(page.titleSuffix) }
                }
            },
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // Subtitle (accent color)
        Text(
            text = page.subtitle,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = page.accentColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(2.dp))

        // Description
        Text(
            text = page.description,
            fontSize = 13.sp,
            color = Color(0xFF777777),
            textAlign = TextAlign.Center,
            lineHeight = 19.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        // Navigation
        NavigationRow(
            current = currentPage,
            total = totalPages,
            accentColor = page.accentColor,
            onPrevious = onPrevious,
            onNext = onNext
        )
    }
}

@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreview() {
    MaterialTheme {
        OnboardingScreen(onFinish = { _ -> })
    }
}
