package com.littlebridge.vidyaprayag.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun BaseScreen(
    onBackClick: (() -> Unit)? = null,

    bottomBar: @Composable () -> Unit = {},

    immersiveTopBar: Boolean = true,

    content: @Composable (
        paddingValues: PaddingValues,
        scrollModifier: Modifier
    ) -> Unit
) {

    val drawerState = rememberDrawerState(
        initialValue = DrawerValue.Closed
    )

    val scope = rememberCoroutineScope()

    // COLLAPSE STATE

    val collapseFraction = remember {
        mutableFloatStateOf(0f)
    }

    // NESTED SCROLL TRACKER

    val nestedScrollConnection = remember {

        object : NestedScrollConnection {

            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {

                if (!immersiveTopBar) {
                    return Offset.Zero
                }

                val delta = available.y

                val newProgress = (
                        collapseFraction.floatValue - (delta / 600f)
                        ).coerceIn(0f, 1f)

                collapseFraction.floatValue = newProgress

                return Offset.Zero
            }
        }
    }

    val isCollapsed = collapseFraction.floatValue > 0.02f

    // ANIMATIONS

    val horizontalPadding by animateDpAsState(
        targetValue = if (immersiveTopBar && isCollapsed) 16.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = ""
    )

    val topPadding by animateDpAsState(
        targetValue = if (immersiveTopBar && isCollapsed) 16.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = ""
    )

    val cornerRadius by animateDpAsState(
        targetValue = if (immersiveTopBar && isCollapsed) 28.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = ""
    )

    val elevation by animateDpAsState(
        targetValue = if (immersiveTopBar && isCollapsed) 10.dp else 0.dp,
        animationSpec = tween(250),
        label = ""
    )

    val topBarColor by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.surface.copy(
            alpha = if (immersiveTopBar && isCollapsed) {
                0.96f
            } else {
                1f
            }
        ),
        animationSpec = tween(250),
        label = ""
    )

    // THIS IS IMPORTANT
    // CONTENT GOES BEHIND TOP BAR WHEN COLLAPSED

    val topContentPadding by animateDpAsState(
        targetValue = if (immersiveTopBar && isCollapsed) {
            0.dp
        } else {
            88.dp
        },
        animationSpec = tween(250),
        label = ""
    )

    ModalNavigationDrawer(
        drawerState = drawerState,

        drawerContent = {
            EduTrustDrawerSheet()
        }
    ) {

        Scaffold(
            modifier = Modifier.fillMaxSize(),

            containerColor = MaterialTheme.colorScheme.background,

            contentWindowInsets = WindowInsets(0),

            bottomBar = {
                Box(
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    bottomBar()
                }
            }

        ) { scaffoldPadding ->

            Box(
                modifier = Modifier.fillMaxSize()
            ) {

                // MAIN CONTENT

                content(

                    PaddingValues(
                        top = topContentPadding.coerceAtLeast(0.dp),
                        bottom = scaffoldPadding.calculateBottomPadding().coerceAtLeast(0.dp)
                    ),

                    if (immersiveTopBar) {
                        Modifier.nestedScroll(nestedScrollConnection)
                    } else {
                        Modifier
                    }
                )

                // TOP BAR

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(
                            horizontal = horizontalPadding.coerceAtLeast(0.dp),
                            vertical = topPadding.coerceAtLeast(0.dp)
                        ),

                    shape = RoundedCornerShape(cornerRadius.coerceAtLeast(0.dp)),

                    color = topBarColor,

                    tonalElevation = elevation.coerceAtLeast(0.dp),

                    shadowElevation = elevation.coerceAtLeast(0.dp)
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(horizontal = 12.dp),

                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        IconButton(
                            onClick = {
                                scope.launch {
                                    drawerState.open()
                                }
                            }
                        ) {

                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu"
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "EduTrust",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        if (onBackClick != null) {

                            IconButton(
                                onClick = onBackClick
                            ) {

                                Icon(
                                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


/*{
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            EduTrustDrawerSheet()
        }
    ) {

        if (isTopBarHovering) {

            // Hovering TopBar Layout
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    Box(
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        bottomBar()
                    }
                },
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0)
            ) { _ ->

                Box(
                    modifier = Modifier.fillMaxSize()
                ) {

                    // Main Content Behind Toolbar
                    content(PaddingValues())

                    // Floating/Hovering TopBar
                    EduTrustTopBar(
                        onMenuClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        },
                        onBackClick = onBackClick,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars)
                    )
                }
            }

        } else {

            // Standard Scaffold Layout
            Scaffold(
                modifier = Modifier.fillMaxSize(),

                topBar = {
                    EduTrustTopBar(
                        onMenuClick = {
                            scope.launch {
                                drawerState.open()
                            }
                        },
                        onBackClick = onBackClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars)
                    )
                },

                bottomBar = {
                    Box(
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        bottomBar()
                    }
                },

                containerColor = MaterialTheme.colorScheme.background

            ) { paddingValues ->

                content(paddingValues)
            }
        }
    }
}*/
