package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.ChatMessage
import com.example.data.model.StudyMode
import com.example.data.model.StudyNote
import com.example.data.model.SubjectType

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long,
    val subject: String,
    val mode: String
) {
    fun toModel(): ChatMessage {
        val subjectEnum = try {
            SubjectType.valueOf(subject)
        } catch (_: Exception) {
            SubjectType.CODING
        }
        val modeEnum = try {
            StudyMode.valueOf(mode)
        } catch (_: Exception) {
            StudyMode.TEACH
        }
        return ChatMessage(
            id = id,
            text = text,
            isUser = isUser,
            timestamp = timestamp,
            subject = subjectEnum,
            mode = modeEnum
        )
    }

    companion object {
        fun fromModel(model: ChatMessage): ChatMessageEntity {
            return ChatMessageEntity(
                id = model.id,
                text = model.text,
                isUser = model.isUser,
                timestamp = model.timestamp,
                subject = model.subject.name,
                mode = model.mode.name
            )
        }
    }
}

@Entity(tableName = "study_notes")
data class StudyNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String,
    val subject: String,
    val timestamp: Long
) {
    fun toModel(): StudyNote {
        val subjectEnum = try {
            SubjectType.valueOf(subject)
        } catch (_: Exception) {
            SubjectType.CODING
        }
        return StudyNote(
            id = id,
            title = title,
            content = content,
            subject = subjectEnum,
            timestamp = timestamp
        )
    }

    companion object {
        fun fromModel(model: StudyNote): StudyNoteEntity {
            return StudyNoteEntity(
                id = model.id,
                title = model.title,
                content = model.content,
                subject = model.subject.name,
                timestamp = model.timestamp
            )
        }
    }
}
