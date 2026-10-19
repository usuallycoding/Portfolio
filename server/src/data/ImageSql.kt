package com.usuallycoding.portfolio.server.data

/** SQL predicates over the `images` table, shared by the image repository and the cleanup. */
object ImageSql {
    /** True when nothing references the image. */
    const val UNREFERENCED = """
        NOT EXISTS (SELECT 1 FROM profile WHERE profile_image_id = images.id OR cover_image_id = images.id)
        AND NOT EXISTS (SELECT 1 FROM social_links WHERE icon_image_id = images.id)
        AND NOT EXISTS (SELECT 1 FROM projects WHERE thumbnail_image_id = images.id)
        AND NOT EXISTS (SELECT 1 FROM project_images WHERE image_id = images.id)"""

    /** True when an anonymous visitor can reach the image through public content. */
    const val PUBLICLY_REFERENCED = """
        EXISTS (SELECT 1 FROM profile WHERE profile_image_id = images.id OR cover_image_id = images.id)
        OR EXISTS (SELECT 1 FROM social_links WHERE icon_image_id = images.id AND is_public = 1)
        OR EXISTS (SELECT 1 FROM projects WHERE thumbnail_image_id = images.id AND is_public = 1)
        OR EXISTS (SELECT 1 FROM project_images pi JOIN projects p ON p.id = pi.project_id
                   WHERE pi.image_id = images.id AND p.is_public = 1)"""
}
