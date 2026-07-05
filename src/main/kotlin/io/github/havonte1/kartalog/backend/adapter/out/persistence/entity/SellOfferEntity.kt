package io.github.havonte1.kartalog.backend.adapter.out.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import org.hibernate.envers.Audited
import org.hibernate.envers.NotAudited
import java.io.Serializable
import java.time.Instant

@Audited
@Entity
@Table(name = "sell_offers", schema = "watcher")
data class SellOfferEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false,
    )
    val product: ProductEntity,
    @Column(name = "seller_name", nullable = false)
    val sellerName: String,
    @Column(name = "seller_location", nullable = false)
    val sellerLocation: String,
    @Column(name = "product_language", nullable = false)
    val productLanguage: String,
    @Column(name = "special", nullable = false)
    val special: String,
    @Column(name = "condition", nullable = false)
    val condition: String,
    @Column(name = "amount", nullable = false)
    val amount: String,
    @Column(name = "price", nullable = false)
    val price: String,
) : Serializable {
    @PrePersist
    fun onPrePersist() {
        createdAt = Instant.now()
        updatedAt = Instant.now()
    }

    @PreUpdate
    fun onPreUpdate() {
        updatedAt = Instant.now()
    }

    @NotAudited
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    @NotAudited
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    companion object {
        private const val serialVersionUID: Long = 1L
    }

    constructor() : this(
        id = null,
        product = ProductEntity(),
        sellerName = "",
        sellerLocation = "",
        productLanguage = "",
        special = "",
        condition = "",
        amount = "",
        price = "",
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SellOfferEntity) return false
        if (id != null && other.id != null) return id == other.id
        return sellerName == other.sellerName &&
            sellerLocation == other.sellerLocation &&
            productLanguage == other.productLanguage &&
            condition == other.condition &&
            amount == other.amount &&
            price == other.price
    }

    override fun hashCode(): Int = id?.hashCode() ?: toString().hashCode()

    override fun toString(): String =
        "SellOfferEntity(id=$id, sellerName='$sellerName', sellerLocation='$sellerLocation', productLanguage='$productLanguage', special='$special', condition='$condition', amount='$amount', price='$price')"

    fun businessEquals(other: SellOfferEntity): Boolean =
        sellerName      == other.sellerName     &&
            sellerLocation  == other.sellerLocation &&
            productLanguage == other.productLanguage &&
            condition       == other.condition      &&
            amount          == other.amount         &&
            price           == other.price          &&
            special         == other.special
}
