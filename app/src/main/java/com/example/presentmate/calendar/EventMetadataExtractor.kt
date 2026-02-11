package com.example.presentmate.calendar

object EventMetadataExtractor {
    
    /**
     * Extracts Subject and Topic from an event title.
     * Supports separators: " - ", " : ", " | "
     * 
     * Example: "Modern History - Anglo Carnatic Wars" 
     * -> Subject: "Modern History", Topic: "Anglo Carnatic Wars"
     * 
     * If no separator is found, returns (title, null).
     */
    fun extract(title: String): Pair<String, String?> {
        if (title.isBlank()) return Pair("", null)
        
        val separators = listOf(" - ", " : ", " | ", ": ", " |")
        
        for (separator in separators) {
            if (title.contains(separator)) {
                val parts = title.split(separator, limit = 2)
                if (parts.size >= 2) {
                    val subject = parts[0].trim()
                    val topic = parts[1].trim()
                    if (subject.isNotEmpty()) {
                        return Pair(subject, topic.ifEmpty { null })
                    }
                }
            }
        }
        
        // Special case: check for single dash if not surrounded by spaces, 
        // but often "GS-1" is the subject. 
        // Let's stick to explicit separators for now to avoid splitting "GS-1" into "GS" and "1".
        
        return Pair(title.trim(), null)
    }
}
