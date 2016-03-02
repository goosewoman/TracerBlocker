package net.primomc.TracerBlocker;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Map;

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

public class PlayerHider
{
    protected Table<Integer, Integer, Boolean> observerEntityMap = HashBasedTable.create();

    public void hidePlayer( Player a, Player b )
    {
        hideEntity( a, b );
    }

    public void showPlayer( Player a, Player b )
    {
        showEntity( a, b );
    }

    private static final PacketType[] ENTITY_PACKETS = { ENTITY_EQUIPMENT, BED, ANIMATION, NAMED_ENTITY_SPAWN, COLLECT, ENTITY_VELOCITY, REL_ENTITY_MOVE, ENTITY_LOOK, ENTITY_MOVE_LOOK, ENTITY_TELEPORT, ENTITY_HEAD_ROTATION, ENTITY_STATUS, ATTACH_ENTITY, ENTITY_METADATA, ENTITY_EFFECT, REMOVE_ENTITY_EFFECT, BLOCK_BREAK_ANIMATION };

    private ProtocolManager manager;

    // Listeners
    private PacketAdapter protocolListener;

    // Current policy

    /**
     * Construct a new entity hider.
     *
     * @param plugin - the plugin that controls this entity hider.
     */
    public PlayerHider( Plugin plugin )
    {
        Preconditions.checkNotNull( plugin, "plugin cannot be NULL." );

        // Save policy
        this.manager = ProtocolLibrary.getProtocolManager();

        // Register events and packet listener
        Bukkit.getServer().getPluginManager().registerEvents( constructBukkit(), plugin );
        manager.addPacketListener( protocolListener = constructProtocol( plugin ) );
    }

    /**
     * Set the visibility status of a given entity for a particular observer.
     *
     * @param observer - the observer player.
     * @param entityID - ID of the entity that will be hidden or made visible.
     * @param visible  - TRUE if the entity should be made visible, FALSE if not.
     * @return TRUE if the entity was visible before this method call, FALSE otherwise.
     */
    protected boolean setVisibility( Player observer, int entityID, boolean visible )
    {
        return !setMembership( observer, entityID, !visible );
    }

    /**
     * Add or remove the given entity and observer entry from the table.
     *
     * @param observer - the player observer.
     * @param entityID - ID of the entity.
     * @param member   - TRUE if they should be present in the table, FALSE otherwise.
     * @return TRUE if they already were present, FALSE otherwise.
     */
    // Helper method
    protected boolean setMembership( Player observer, int entityID, boolean member )
    {
        if ( member )
        {
            return observerEntityMap.put( observer.getEntityId(), entityID, true ) != null;
        }
        else
        {
            return observerEntityMap.remove( observer.getEntityId(), entityID ) != null;
        }
    }

    /**
     * Determine if the given entity and observer is present in the table.
     *
     * @param observer - the player observer.
     * @param entityID - ID of the entity.
     * @return TRUE if they are present, FALSE otherwise.
     */
    protected boolean getMembership( Player observer, int entityID )
    {
        return observerEntityMap.contains( observer.getEntityId(), entityID );
    }

    /**
     * Construct the Bukkit event listener.
     *
     * @return Our listener.
     */
    private Listener constructBukkit()
    {
        return new Listener()
        {

            @EventHandler
            public void onPlayerQuit( PlayerQuitEvent e )
            {
                removePlayer( e.getPlayer() );
            }
        };
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

    protected void removeEntity( int entityID )
    {
        for ( Map<Integer, Boolean> maps : observerEntityMap.rowMap().values() )
        {
            maps.remove( entityID );
        }
    }

    /**
     * Invoked when a player logs out.
     *
     * @param player - the player that jused logged out.
     */
    protected void removePlayer( Player player )
    {
        observerEntityMap.rowMap().remove( player.getEntityId() );
        removeEntity( player.getEntityId() );
    }

    /**
     * Construct the packet listener that will be used to intercept every entity-related packet.
     *
     * @param plugin - the parent plugin.
     * @return The packet listener.
     */
    private PacketAdapter constructProtocol( Plugin plugin )
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
     * Allow the observer to see an entity that was previously hidden.
     *
     * @param observer - the observer.
     * @param entity   - the entity to show.
     * @return TRUE if the entity was hidden before, FALSE otherwise.
     */
    public final boolean showEntity( Player observer, Entity entity )
    {
        validate( observer, entity );
        boolean hiddenBefore = !setVisibility( observer, entity.getEntityId(), true );

        // Resend packets
        if ( manager != null && hiddenBefore )
        {
            manager.updateEntity( entity, Arrays.asList( observer ) );
        }
        return hiddenBefore;
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

    /**
     * Determine if the given entity has been hidden from an observer.
     * <p/>
     * Note that the entity may very well be occluded or out of range from the perspective
     * of the observer. This method simply checks if an entity has been completely hidden
     * for that observer.
     *
     * @param observer - the observer.
     * @param entity   - the entity that may be hidden.
     * @return TRUE if the player may see the entity, FALSE if the entity has been hidden.
     */
    public final boolean canSee( Player observer, Entity entity )
    {
        validate( observer, entity );

        return isVisible( observer, entity.getEntityId() );
    }

    // For valdiating the input parameters
    private void validate( Player observer, Entity entity )
    {
        Preconditions.checkNotNull( observer, "observer cannot be NULL." );
        Preconditions.checkNotNull( entity, "entity cannot be NULL." );
    }

}
