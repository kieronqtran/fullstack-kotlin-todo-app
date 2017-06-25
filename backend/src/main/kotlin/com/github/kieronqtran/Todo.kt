package com.github.kieronqtran

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import javax.persistence.*

@Entity
@JsonIgnoreProperties(ignoreUnknown=true)
data class Todo(
        var title:String = "",
        var completed:Boolean = false,
        @Column(name = "todoOrder")
        var order:Int = -1,
        @Id @GeneratedValue(strategy = GenerationType.AUTO)
        var id: Long = 0) {

    val url: String
        @JsonProperty
        get()="${Application.Config.root}/$id"

    override fun toString(): String {
        return "Todo(id=$id, title=$title, completed=$completed, order=$order, url=$url)"
    }
}
