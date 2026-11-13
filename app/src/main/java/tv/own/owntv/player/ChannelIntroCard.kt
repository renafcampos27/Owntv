package tv.own.owntv.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

/** Non-focusable channel introduction, without transport, clocks, logos or technical metadata. */
@Composable
internal fun ChannelIntroCard(channel: String, programme: String?, description: String?) {
    Column(Modifier.fillMaxWidth(0.72f).clip(RoundedCornerShape(14.dp))
        .background(Color.Black.copy(alpha = 0.78f)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(channel, style = MaterialTheme.typography.titleMedium, color = Color.White,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
        programme?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodyLarge, color = Color.White,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        description?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f),
                maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}
