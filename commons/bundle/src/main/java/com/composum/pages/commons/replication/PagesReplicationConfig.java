package com.composum.pages.commons.replication;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Composum Pages Replication Configuration"
)
public @interface PagesReplicationConfig {

    @AttributeDefinition(
            name = "InPlace enabled",
            description = "the on/off switch for the 'InPlace' replication strategy"
    )
    boolean inPlaceEnabled() default true;

    @AttributeDefinition(
            name = "InPlace Preview path",
            description = "the repository root of the 'preview' replication content; default '/preview'"
    )
    String inPlacePreviewPath() default "/preview";

    @AttributeDefinition(
            name = "InPlace Public path",
            description = "the repository root of the 'public' replication content; default '/public'"
    )
    String inPlacePublicPath() default "/public";

    @AttributeDefinition(
            name = "Content path",
            description = "the repository root of the authoring content to replicate; default '/content'"
    )
    String contentPath() default "/content";
}
