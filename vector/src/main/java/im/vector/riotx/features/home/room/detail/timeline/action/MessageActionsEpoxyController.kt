/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package im.vector.riotx.features.home.room.detail.timeline.action

import android.view.View
import com.airbnb.epoxy.TypedEpoxyController
import com.airbnb.mvrx.Success
import im.vector.riotx.EmojiCompatFontProvider
import im.vector.riotx.R
import im.vector.riotx.core.epoxy.bottomsheet.BottomSheetItemQuickReactions
import im.vector.riotx.core.epoxy.bottomsheet.bottomSheetItemAction
import im.vector.riotx.core.epoxy.bottomsheet.bottomSheetItemMessagePreview
import im.vector.riotx.core.epoxy.bottomsheet.bottomSheetItemQuickReactions
import im.vector.riotx.core.epoxy.bottomsheet.bottomSheetItemSendState
import im.vector.riotx.core.epoxy.bottomsheet.bottomSheetItemSeparator
import im.vector.riotx.core.resources.StringProvider
import im.vector.riotx.features.home.AvatarRenderer
import javax.inject.Inject

/**
 * Epoxy controller for message action list
 */
class MessageActionsEpoxyController @Inject constructor(private val stringProvider: StringProvider,
                                                        private val avatarRenderer: AvatarRenderer,
                                                        private val fontProvider: EmojiCompatFontProvider) : TypedEpoxyController<MessageActionState>() {

    var listener: MessageActionsEpoxyControllerListener? = null

    override fun buildModels(state: MessageActionState) {
        // Message preview
        val body = state.messageBody
        if (body != null) {
            bottomSheetItemMessagePreview {
                id("preview")
                avatarRenderer(avatarRenderer)
                avatarUrl(state.informationData.avatarUrl ?: "")
                senderId(state.informationData.senderId)
                senderName(state.senderName())
                body(body)
                time(state.time())
            }
        }

        // Send state
        if (state.informationData.sendState.isSending()) {
            bottomSheetItemSendState {
                id("send_state")
                showProgress(true)
                text(stringProvider.getString(R.string.event_status_sending_message))
            }
        } else if (state.informationData.sendState.hasFailed()) {
            bottomSheetItemSendState {
                id("send_state")
                showProgress(false)
                text(stringProvider.getString(R.string.unable_to_send_message))
                drawableStart(R.drawable.ic_warning_small)
            }
        }

        // Quick reactions
        if (state.canReact() && state.quickStates is Success) {
            // Separator
            bottomSheetItemSeparator {
                id("reaction_separator")
            }

            bottomSheetItemQuickReactions {
                id("quick_reaction")
                fontProvider(fontProvider)
                texts(state.quickStates()?.map { it.reaction }.orEmpty())
                selecteds(state.quickStates.invoke().map { it.isSelected })
                listener(object : BottomSheetItemQuickReactions.Listener {
                    override fun didSelect(emoji: String, selected: Boolean) {
                        listener?.didSelectMenuAction(EventSharedAction.QuickReact(state.eventId, emoji, selected))
                    }
                })
            }
        }

        // Separator
        bottomSheetItemSeparator {
            id("actions_separator")
        }

        // Action
        state.actions()?.forEachIndexed { index, action ->
            bottomSheetItemAction {
                id("action_$index")
                iconRes(action.iconResId)
                textRes(action.titleRes)
                showExpand(action is EventSharedAction.ReportContent)
                expanded(state.expendedReportContentMenu)
                listener(View.OnClickListener { listener?.didSelectMenuAction(action) })
            }

            if (action is EventSharedAction.ReportContent && state.expendedReportContentMenu) {
                // Special case for report content menu: add the submenu
                listOf(
                        EventSharedAction.ReportContentSpam(action.eventId, action.senderId),
                        EventSharedAction.ReportContentInappropriate(action.eventId, action.senderId),
                        EventSharedAction.ReportContentCustom(action.eventId, action.senderId)
                ).forEachIndexed { indexReport, actionReport ->
                    bottomSheetItemAction {
                        id("actionReport_$indexReport")
                        subMenuItem(true)
                        iconRes(actionReport.iconResId)
                        textRes(actionReport.titleRes)
                        listener(View.OnClickListener { listener?.didSelectMenuAction(actionReport) })
                    }
                }
            }
        }
    }

    interface MessageActionsEpoxyControllerListener {
        fun didSelectMenuAction(eventAction: EventSharedAction)
    }
}
