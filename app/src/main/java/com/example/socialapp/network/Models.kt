package com.example.socialapp.network

import androidx.annotation.Keep
import java.io.Serializable


@Keep
data class ListTopic(
    val id: String?,
    val slug: String?,
    val title: String?,
    val cover_photo: CoverPhoto?,
    val preview_photos: List<PreviewPhoto>
)

@Keep
data class ListPhoto(
    val id: String?,
    val created_at: String?,
    val updated_at: String?,
    val width: Int?,
    val height: Int?,
    val color: String?,
    val blur_hash: String?,
    val likes: Int?,
    val liked_by_user: Boolean?,
    val description: String?,
    val user: User?,
    val current_user_collections: List<UserCollections>,
    val urls: ImageUrl?,
    val links: Link?
): Serializable

@Keep
class PreviewPhoto(
    val id: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val urls: ImageUrl? = null,
    val blur_hash: String?,
): Serializable

@Keep
class Location(
    val city: String?,
    val country: String?,
    val position: Position?
): Serializable

@Keep
class Position(
    val latitude: Float?,
    val longitude: Float?
): Serializable

@Keep
class Exif(
    val make: String?,
    val model: String?,
    val name: String?,
    val exposure_time: String?,
    val aperture: String?,
    val focal_length: String?,
    val iso: Int?
): Serializable

@Keep
class User(
    val id: String?,
    val username: String?,
    val name: String?,
    val portfolio_url: String?,
    val bio: String?,
    val location: String?,
    val total_likes: Int?,
    val total_photos: Int?,
    val total_collections: Int?,
    val instagram_username: String?,
    val twitter_username: String?,
    val profile_image: ProfileImage,
//    val links: List<Link>
): Serializable

@Keep
class ProfileImage(
    val small: String?,
    val medium: String?,
    val large: String?
): Serializable

@Keep
class Link(
    val self: String?,
    val html: String?,
    val photos: String?,
    val likes: String?,
    val portfolio: String?,
    val download_location: String?
): Serializable

@Keep
class UserCollections(
    val id: Int?,
    val title: String?,
    val published_at: String?,
    val last_collected_at: String?,
    val updated_at: String?,
    val cover_photo: CoverPhoto?,
    val user: User?
): Serializable

@Keep
class CoverPhoto(
    val id: String?,
    val created_at: String?,
    val updated_at: String?,
    val promoted_at: String?,
    val width: Int?,
    val height: Int?,
    val color: String?,
    val blur_hash: String?,
    val description: String?,
    val alt_description: String?,
    val urls: ImageUrl?,
    val user: User?,
    val location: String?,
    val current_user_collections: List<UserCollections>
): Serializable

@Keep
class ImageUrl(
    val raw: String?,
    val full: String?,
    val regular: String?,
    val small: String?,
    val thumb: String?
): Serializable