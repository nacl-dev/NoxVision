package com.noxvision.app.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noxvision.app.R
import com.noxvision.app.ui.NightColors

@Composable
fun DarkButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    isRecording: Boolean = false,
    isLoading: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .height(48.dp)
            .then(
                if (isLoading) Modifier.semantics { stateDescription = stringResource(R.string.loading) } else Modifier
            ),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isRecording) {
                NightColors.recording
            } else if (enabled) {
                NightColors.primaryDim
            } else {
                NightColors.surface
            },
            contentColor = if (enabled) NightColors.onSurface else NightColors.onBackground.copy(
                alpha = 0.4f
            ),
            disabledContainerColor = if (isLoading) NightColors.primaryDim else NightColors.surface,
            disabledContentColor = if (isLoading) NightColors.onSurface else NightColors.onBackground.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = NightColors.onSurface,
                strokeWidth = 2.dp
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
        }
        if (text.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
