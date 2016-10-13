package net.primomc.TracerBlocker.PlayerHider;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;

import static com.comphenix.protocol.PacketType.Play.Server.*;

/**
 * Copyright ${year} Luuk Jacobs
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@SuppressWarnings( "Duplicates" )
public class PlayerHider1_9 extends AbstractPlayerHider
{

    private static final PacketType[] ENTITY_PACKETS = { ENTITY_EQUIPMENT, BED, ANIMATION, NAMED_ENTITY_SPAWN, COLLECT, ENTITY_VELOCITY, REL_ENTITY_MOVE, ENTITY_LOOK, ENTITY_MOVE_LOOK, ENTITY_TELEPORT, ENTITY_HEAD_ROTATION, ENTITY_STATUS, ATTACH_ENTITY, ENTITY_METADATA, ENTITY_EFFECT, REMOVE_ENTITY_EFFECT, BLOCK_BREAK_ANIMATION };

    /**
     * Construct a new entity hider.
     *
     * @param plugin - the plugin that controls this entity hider.
     */
    public PlayerHider1_9( Plugin plugin )
    {
        super( plugin );
    }

    /**
     * Determine if a given entity is visible for a particular observer.
     *
     * @param observer - the observer player.
     * @param entityID -  ID of the entity that we are testing for visibility.
     * @return TRUE if the entity is visible, FALSE otherwise.
     */
    protected boolean isVisible( Player observer, int entityID )
    {
        // If we are using a whitelist, presence means visibility - if not, the opposite is the case

        return !getMembership( observer, entityID );
    }

    /**
     * Construct the packet listener that will be used to intercept every entity-related packet.
     *
     * @param plugin - the parent plugin.
     * @return The packet listener.
     */
    protected PacketAdapter constructProtocol( Plugin plugin )
    {
        return new PacketAdapter( plugin, ENTITY_PACKETS )
        {
            @Override
            public void onPacketSending( PacketEvent event )
            {
                int entityID = event.getPacket().getIntegers().read( 0 );

                // See if this packet should be cancelled
                if ( !isVisible( event.getPlayer(), entityID ) )
                {
                    event.setCancelled( true );
                }
            }
        };
    }

    /**
     * Prevent the observer from seeing a given entity.
     *
     * @param observer - the player observer.
     * @param entity   - the entity to hide.
     * @return TRUE if the entity was previously visible, FALSE otherwise.
     */
    public final boolean hideEntity( Player observer, Entity entity )
    {
        validate( observer, entity );
        boolean visibleBefore = setVisibility( observer, entity.getEntityId(), false );

        if ( visibleBefore )
        {
            PacketContainer destroyEntity = new PacketContainer( ENTITY_DESTROY );
            destroyEntity.getIntegerArrays().write( 0, new int[]{ entity.getEntityId() } );

            // Make the entity disappear
            try
            {
                manager.sendServerPacket( observer, destroyEntity );
            }
            catch ( InvocationTargetException e )
            {
                throw new RuntimeException( "Cannot send server packet.", e );
            }
        }
        return visibleBefore;
    }

}
