package com.littlebridge.vidyaprayag.presentation.landing

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.littlebridge.vidyaprayag.ui.components.*
import com.littlebridge.vidyaprayag.feature.schools.domain.model.School
import org.koin.compose.viewmodel.koinViewModel
import com.littlebridge.vidyaprayag.presentation.MainViewModel
import com.littlebridge.vidyaprayag.domain.util.UiState
import com.littlebridge.vidyaprayag.navigation.LocalAppNavigator
import com.littlebridge.vidyaprayag.navigation.Destination
import com.littlebridge.vidyaprayag.ui.auth.AuthBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonLandingScreen() {
    val viewModel: MainViewModel = koinViewModel()
    val schoolsState by viewModel.schools.collectAsState()
    val navigator = LocalAppNavigator.current
    
    var showAuthSheet by remember { mutableStateOf(false) }

    BaseScreen { paddingValues, scrollModifier ->
        if (showAuthSheet) {
            AuthBottomSheet(onDismissRequest = { showAuthSheet = false })
        }
        
        when (val state = schoolsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
            }
            is UiState.Success -> {
                val schools = state.data
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(scrollModifier)
                        .padding(paddingValues),
                    contentPadding = PaddingValues(top = 80.dp, bottom = 32.dp)
                ) {
                    item { 
                        HeroSection(onSearchClick = { navigator.navigateTo(Destination.Search) }) 
                    }
                    
                    if (schools.isNotEmpty()) {
                        item { 
                            FeaturedSchoolsSection(schools, onSchoolClick = { id -> 
                                navigator.navigateTo(Destination.SchoolDetails(id)) 
                            }) 
                        }
                    }

                    item { SocialProofSection() }
                    item { EntryPointsSection(onJoinClick = { showAuthSheet = true }) }
                    item { MoatShowcaseSection() }
                    //item { PortalAccessSection(onLoginClick = { showAuthSheet = true }) }
                    item { FinalCtaSection(onJoinClick = { showAuthSheet = true }) }
                    item { FooterSection() }
                }
            }
        }
    }
}

@Composable
private fun FeaturedSchoolsSection(
    schools: List<School>,
    onSchoolClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(24.dp)) {
        Text(
            "Featured Institutions",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            schools.forEach { school ->
                SchoolCard(school = school, onClick = { onSchoolClick(school.id) })
            }
        }
    }
}

@Composable
private fun SchoolCard(
    school: School,
    onClick: () -> Unit
) {
    EduTrustCard(
        modifier = Modifier.width(240.dp).clickable { onClick() }
    ) {
        Column {
            AsyncImage(
                model = school.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(120.dp),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Text(school.name, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(school.location, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(8.dp))
                Text(school.board, fontSize = 10.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun HeroSection(onSearchClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Education with Trust.",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            lineHeight = 44.sp
        )
        Text(
            text = "Progress with Purpose.",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            lineHeight = 44.sp
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        AsyncImage(
            model = "https://lh3.googleusercontent.com/aida/ADBb0ujKS0F1JiULtLeeVDpTqgaNyFbwA67q0g2mU5kpdJ3STuxY-9WZXhkeDtjdHaEErpJQ2WGLrgQBMs8LG5ZxDw45A_TiYvX37WedwysCnF5r2iOJHOitbJg5S0uwgXuTeU1jGHtw6cEuOj-pNLPrdwJh92Cr8i2q0fXbSuAv0HWfUBbUGilE3F8PgjdfdfEe3SesTla-LxmAJWgi6JfGq4p3gTnHdmAkzetEsjjgZGiwHlacf8cCz-NRhWs",
            contentDescription = null,
            modifier = Modifier
                .size(320.dp)
                .clip(RoundedCornerShape(40.dp))
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(40.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(32.dp))
        
        EduTrustSearchBar(
            modifier = Modifier.padding(horizontal = 16.dp),
            onSearchClick = onSearchClick
        )
    }
}

@Composable
private fun SocialProofSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
    ) {
        Text(
            "TRUSTED BY 500+ INSTITUTIONS",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.outline,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MarqueeItem(Icons.Default.AccountBalance, "Academix")
            MarqueeItem(Icons.Default.Token, "GlobalView")
            MarqueeItem(Icons.Default.Layers, "EduPulse")
        }
    }
}

@Composable
private fun MarqueeItem(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.alpha(0.6f)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun EntryPointsSection(onJoinClick: () -> Unit) {
    Column(modifier = Modifier.padding(24.dp)) {
        EntryPointCard(
            label = "FOR PARENTS",
            title = "Find the perfect school for your child\'s unique journey",
            description = "Empowering parents with data-driven insights and verified institutional profiles.",
            features = listOf("Verified institutional profiles", "Smart Comparison highlights", "AI Career Paths & Talent ID"),
            buttonText = "Start Your Search",
            onButtonClick = onJoinClick
        )
        Spacer(modifier = Modifier.height(24.dp))
        EntryPointCard(
            label = "FOR SCHOOLS",
            title = "Scale excellence with intelligence.",
            description = "Advanced institutional management tools designed for modern educational growth.",
            features = listOf("Full Admissions CRM", "Teacher Accountability", "Automated Compliance"),
            buttonText = "Onboard Your School",
            onButtonClick = onJoinClick
        )
    }
}

@Composable
private fun EntryPointCard(
    label: String, 
    title: String, 
    description: String, 
    features: List<String>, 
    buttonText: String,
    onButtonClick: () -> Unit
) {
    EduTrustCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(32.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(label, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(title, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(24.dp))
            features.forEach { feature ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(feature, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            EduTrustPrimaryButton(
                text = buttonText,
                onClick = onButtonClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MoatShowcaseSection() {
    Column(modifier = Modifier.padding(vertical = 40.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text("Next-Gen Intelligence", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
            Text("Proprietary systems powering the ecosystem.", color = MaterialTheme.colorScheme.outline)
        }
        Spacer(modifier = Modifier.height(32.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 24.dp)
        ) {
            MoatCard("WhatsApp-First", "Seamless communication between parents and faculty without app fatigue.", "https://lh3.googleusercontent.com/aida/ADBb0uhupW5U-PHnHTPXDYeq9A90Omu-E8beJf0uK7eTRa-L8daRVU5rvILYqec9IZN74A8Y3KFYBo3z_7iRzrZiZhb_Zvpe2YH0_1xFY06JNxAcgV57Zvaf80QtV7PjL4UeBY-zJLUw1iODOqF5uHXssbGzkhmW4NnHlnXxYLpnK1hYG3zeHtky2MNngvRtWCWa6oR3KaTLKFAw_eGHOL_p0t60JAn5Ha52Sr5FfKiB7e7slDLCphIIUa_y2m7n")
            Spacer(modifier = Modifier.width(16.dp))
            MoatCard("SRI Index", "Standardized Reliability Index for objective school performance tracking.", "https://lh3.googleusercontent.com/aida/ADBb0uja34Re_-MtOF9jh5ZyVhQGKS4GfxPzJYtBhBlW10Xem3awSStEWcQapUQMn84PxpJewsaPADpJFUHEmmurRCYaMQxn0RrEMUfKnhgm5x3e5L9NVqRF2PYk3JLfBHm3wWG-9FO94L6Jfs9G9hvcp3m8H9AaL9HhsNrARYaA6ptaWgvQCqXhGxbZi53-E2MLeaH0zRuQxWq_uOFhJXfrZhZ3jYiOErFrXZwdVHYDZTVj-ULoIjrXMisgtdSn")
        }
    }
}

@Composable
private fun MoatCard(title: String, description: String, imageUrl: String) {
    EduTrustCard(modifier = Modifier.width(300.dp)) {
        Column(modifier = Modifier.padding(32.dp)) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(16.dp))
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            Text(description, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
private fun PortalAccessSection(onLoginClick: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 40.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Column {
                Text("Access Your Portal", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                Text("Already part of the EduTrust ecosystem?", color = MaterialTheme.colorScheme.outline)
            }
            Text("View All Portals", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(32.dp))
        PortalCard(Icons.Default.FamilyRestroom, "Parent Portal", "Track progress, monitor safety, and connect with faculty.", onLoginClick)
        Spacer(modifier = Modifier.height(16.dp))
        PortalCard(Icons.Default.AdminPanelSettings, "Admin Center", "Manage institutional compliance and staff performance.", onLoginClick)
        Spacer(modifier = Modifier.height(16.dp))
        PortalCard(Icons.Default.AssignmentInd, "Teacher App", "Update attendance, grades, and student reports daily.", onLoginClick)
    }
}

@Composable
private fun PortalCard(icon: ImageVector, title: String, description: String, onLoginClick: () -> Unit) {
    EduTrustCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        elevation = 0
    ) {
        Column(modifier = Modifier.padding(32.dp)) {
            Box(
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(description, color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(24.dp))
            EduTrustOutlinedButton(
                text = "Log In",
                onClick = onLoginClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun FinalCtaSection(onJoinClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .clip(RoundedCornerShape(48.dp))
            .height(400.dp)
    ) {
        AsyncImage(
            model = "https://lh3.googleusercontent.com/aida/ADBb0uiTft_c1_2MWVAWhm4Gox-ivfML7QYXvMPpzM8A9tXfKXepAlvpYOWI2PW4VYKTMkkxqyoJvdNqB_4RkM_bitptyKAQevv2M2RXO5AkhEYqdSpODppC4zbkBa67wQV-Y2fnTzxu9mFzmzDLt6cgO_iL9rwKHMDkPBbg2q4V0KQlw2tQvst-vE4izvr5-pNPojgtX1uNI3kXK6qvzGMLXCIgav2X-mKVofJBiwU-XpuR9deYAwIaZiNHEVvn",
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.4f
        )
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)))
        
        Column(
            modifier = Modifier.fillMaxSize().padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Ready to secure the future?", style = MaterialTheme.typography.headlineLarge, color = Color.White, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))
            Text("Join the ecosystem where trust meets technology. Join over 50k+ parents today.", color = MaterialTheme.colorScheme.onPrimaryContainer, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(40.dp))
            EduTrustSecondaryButton(
                text = "Join as a Parent",
                onClick = onJoinClick,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            EduTrustOutlinedButton(
                text = "Register Your School",
                onClick = onJoinClick,
                modifier = Modifier.fillMaxWidth(),
                borderColor = Color.White.copy(alpha = 0.2f),
                contentColor = Color.White
            )
        }
    }
}

@Composable
private fun FooterSection() {
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(32.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("EduTrust", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "© 2024 EduTrust Ecosystem. Built for modern institutional excellence and parent-child security. Innovating education through trust.",
            color = Color.Gray,
            lineHeight = 24.sp
        )
        Spacer(modifier = Modifier.height(48.dp))
        FooterLinkSection("PORTALS", listOf("Parent Portal", "Admin Dashboard", "Teacher Console"))
        Spacer(modifier = Modifier.height(48.dp))
        FooterLinkSection("SUPPORT", listOf("Privacy Policy", "Terms of Service", "Help Desk"))
    }
}

@Composable
private fun FooterLinkSection(title: String, links: List<String>) {
    Column {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, letterSpacing = 2.sp)
        Spacer(modifier = Modifier.height(24.dp))
        links.forEach { link ->
            Text(link, color = Color.Gray, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
