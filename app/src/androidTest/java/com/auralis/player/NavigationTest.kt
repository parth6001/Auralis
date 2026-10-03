package com.auralis.player

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class NavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun firstRunAndSearchHaveAccessibleActions() {
        compose.onNodeWithText("Add folder").assertHasClickAction().assertIsDisplayed()
        compose.onNodeWithText("Search").performClick()
        compose.onNodeWithText("Search songs, albums, artists").performTextInput("No such song")
        compose.onNodeWithText("Home").performClick()
        compose.onNodeWithText("Privacy & settings").performClick()
        compose.onNodeWithText("AMOLED").performScrollTo().performClick()
        compose.onNodeWithText("Home").performClick()
        compose.onNodeWithText("Add folder").assertIsDisplayed()
    }
}
