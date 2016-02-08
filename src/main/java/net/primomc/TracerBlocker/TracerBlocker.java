package net.primomc.TracerBlocker;

import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import net.primomc.TracerBlocker.PacketWrapper.WrapperPlayServerBlockChange;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BlockIterator;

import java.util.*;

/*
 * Copyright 2016 Luuk Jacobs

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class TracerBlocker extends JavaPlugin
{


    private static final Random rand = new Random();

    @Override
    public void onEnable()
    {

        loadConfig();

        if ( Settings.PlayerHider.enabled )
        {
            getServer().getScheduler().runTaskTimer( this, new Runnable()
            {
                @Override
                public void run()
                {
                    checkVisibility();
                }
            }, 100, Settings.PlayerHider.everyTicks );
        }
        if ( Settings.ChestHider.enabled )
        {
            getServer().getScheduler().runTaskTimer( this, new Runnable()
            {
                @Override
                public void run()
                {
                    checkChestVisibility();
                }
            }, 100, Settings.ChestHider.everyTicks );
        }
        if ( Settings.FakePlayers.enabled )
        {
            getServer().getScheduler().runTaskTimer( this, new Runnable()
            {
                @Override
                public void run()
                {
                    spawnFakePlayers();
                }
            }, 100, Settings.FakePlayers.everyTicks );
        }
    }

    private void loadConfig()
    {
        saveDefaultConfig();
        Settings.PlayerHider.enabled = getConfig().getBoolean( "playerhider.enabled" );
        Settings.PlayerHider.everyTicks = getConfig().getInt( "playerhider.every-ticks" );
        Settings.PlayerHider.ignoreDistance = getConfig().getInt( "playerhider.ignore-distance" );
        Settings.PlayerHider.maxDistance = getConfig().getInt( "playerhider.max-distance" );
        Settings.PlayerHider.disabledWorlds = getConfig().getStringList( "playerhider.disabledWorlds" );

        Settings.ChestHider.enabled = getConfig().getBoolean( "chesthider.enabled" );
        Settings.ChestHider.everyTicks = getConfig().getInt( "chesthider.every-ticks" );
        Settings.ChestHider.ignoreDistance = getConfig().getInt( "chesthider.ignore-distance" );
        Settings.ChestHider.maxDistance = getConfig().getInt( "chesthider.max-distance" );
        Settings.ChestHider.disabledWorlds = getConfig().getStringList( "chesthider.disabledWorlds" );

        Settings.FakePlayers.enabled = getConfig().getBoolean( "fakeplayers.enabled" );
        Settings.FakePlayers.everyTicks = getConfig().getInt( "fakeplayers.every-ticks" );
        Settings.FakePlayers.secondsAlive = getConfig().getInt( "fakeplayers.seconds-alive" );
        Settings.FakePlayers.speed = getConfig().getInt( "fakeplayers.speed" );
        Settings.FakePlayers.disabledWorlds = getConfig().getStringList( "fakeplayers.disabledWorlds" );
    }

    private void checkChestVisibility()
    {
        for ( Player a : Bukkit.getOnlinePlayers() )
        {
            Location loc = a.getLocation();
            World world = loc.getWorld();
            if ( Settings.ChestHider.disabledWorlds.contains( world.getName() ) )
            {
                continue;
            }
            int chunkRadius = Settings.ChestHider.maxDistance / 16;
            int minX = loc.getChunk().getX() - chunkRadius;
            int maxX = loc.getChunk().getX() + chunkRadius;
            int minZ = loc.getChunk().getZ() - chunkRadius;
            int maxZ = loc.getChunk().getZ() + chunkRadius;
            for ( int x = minX; x < maxX; x++ )
            {
                for ( int z = minZ; z < maxZ; z++ )
                {
                    Chunk chunk = world.getChunkAt( x, z );
                    for ( BlockState state : chunk.getTileEntities() )
                    {
                        if ( state.getType().equals( Material.CHEST ) || state.getType().equals( Material.TRAPPED_CHEST ) || state.getType().equals( Material.ENDER_CHEST ) )
                        {
                            double size = .90;
                            Location targetAA = state.getLocation().clone().add( 0, 0, 0 );
                            Location targetBB = state.getLocation().clone().add( size, 0, 0 );
                            Location targetCC = state.getLocation().clone().add( size, 0, size );
                            Location targetDD = state.getLocation().clone().add( 0, 0, size );
                            Location targetEE = state.getLocation().clone().add( 0, size, 0 );
                            Location targetFF = state.getLocation().clone().add( size, size, 0 );
                            Location targetGG = state.getLocation().clone().add( size, size, size );
                            Location targetHH = state.getLocation().clone().add( 0, size, size );

                            int distance = (int) a.getLocation().distance( targetAA );

                            // No need to check this
                            if ( distance > Settings.ChestHider.maxDistance )
                            {
                                continue;
                            }

                            if ( distance <= Settings.ChestHider.ignoreDistance )
                            {
                                showBlock( a, state.getLocation() );
                                continue;
                            }

                            Block blockAA = getTargetBlock( lookAt( a.getEyeLocation(), targetAA ), distance );
                            Block blockBB = getTargetBlock( lookAt( a.getEyeLocation(), targetBB ), distance );
                            Block blockCC = getTargetBlock( lookAt( a.getEyeLocation(), targetCC ), distance );
                            Block blockDD = getTargetBlock( lookAt( a.getEyeLocation(), targetDD ), distance );

                            Block blockEE = getTargetBlock( lookAt( a.getEyeLocation(), targetEE ), distance );
                            Block blockFF = getTargetBlock( lookAt( a.getEyeLocation(), targetFF ), distance );
                            Block blockGG = getTargetBlock( lookAt( a.getEyeLocation(), targetGG ), distance );
                            Block blockHH = getTargetBlock( lookAt( a.getEyeLocation(), targetHH ), distance );

                            if ( blockAA == null || blockAA.getType().equals( state.getBlock().getType() ) || blockBB == null || blockBB.getType().equals( state.getBlock().getType() ) || blockCC == null || blockCC.getType().equals( state.getBlock().getType() ) || blockDD == null || blockDD.getType().equals( state.getBlock().getType() ) || blockEE == null || blockEE.getType().equals( state.getBlock().getType() ) || blockFF == null || blockFF.getType().equals( state.getBlock().getType() ) || blockGG == null || blockGG.getType().equals( state.getBlock().getType() ) || blockHH == null || blockHH.getType().equals( state.getBlock().getType() ) )
                            {
                                showBlock( a, state.getLocation() );
                            }
                            else
                            {
                                hideBlock( a, state.getLocation() );
                            }
                        }
                    }
                }
            }
        }
    }

    private void showBlock( Player player, Location location )
    {
        changeBlock( player, location, location.getBlock().getType(), location.getBlock().getData() );
    }

    private void changeBlock( Player player, Location location, Material type, byte data )
    {
        WrapperPlayServerBlockChange packet = new WrapperPlayServerBlockChange();
        packet.setLocation( new BlockPosition( location.getBlockX(), location.getBlockY(), location.getBlockZ() ) );
        packet.setBlockData( WrappedBlockData.createData( type, data ) );
        packet.sendPacket( player );
    }

    private void hideBlock( Player player, Location location )
    {
        changeBlock( player, location, Material.AIR, (byte) 0 );
    }

    private void spawnFakePlayers()
    {
        for ( Player player : Bukkit.getOnlinePlayers() )
        {
            if ( Settings.FakePlayers.disabledWorlds.contains( player.getLocation().getWorld().getName() ) )
            {
                continue;
            }
            Location fakeLocation;
            do
            {
                int x = 0, y = 0, z = 0;
                while ( x < 16 && x > -16 )
                {
                    x = rand( -40, 40 );
                }
                while ( y < 10 && y > -10 )
                {
                    y = rand( -30, 20 );
                }
                while ( z < 16 && z > -16 )
                {
                    z = rand( -40, 40 );
                }
                fakeLocation = player.getLocation().clone().add( x, y, z );
            }
            while ( fakeLocation.distance( player.getLocation() ) < 16 );
            new FakePlayer( this, fakeLocation ).addObserver( player );
        }
    }

    public static int rand( int min, int max )
    {
        return min + ( rand ).nextInt( max - min );
    }

    private void checkVisibility()
    {
        for ( Player a : Bukkit.getOnlinePlayers() )
        {
            for ( Player b : Bukkit.getOnlinePlayers() )
            {
                if ( a.equals( b ) )
                {
                    continue;
                }
                if ( a.getWorld().equals( b.getWorld() ) )
                {
                    if ( Settings.PlayerHider.disabledWorlds.contains( a.getWorld().getName() ) )
                    {
                        continue;
                    }
                    double width = 0.45;
                    Location targetAA = b.getLocation().clone().add( -width, 0, -width );
                    Location targetBB = b.getLocation().clone().add( width, 1.9, width );
                    Location targetCC = b.getLocation().clone().add( 0, 1.1, 0 );
                    int distance = (int) a.getLocation().distance( targetAA );

                    if ( distance > Settings.PlayerHider.maxDistance )
                    {
                        continue;
                    }

                    if ( distance <= Settings.PlayerHider.ignoreDistance )
                    {
                        showPlayer( a, b );
                        continue;
                    }
                    if ( getTargetBlock( lookAt( a.getEyeLocation(), targetAA ), distance ) == null || getTargetBlock( lookAt( a.getEyeLocation(), targetBB ), distance ) == null || getTargetBlock( lookAt( a.getEyeLocation(), targetCC ), distance ) == null )
                    {
                        showPlayer( a, b );
                    }
                    else
                    {
                        hidePlayer( a, b );
                    }
                }
            }
        }
    }

    Map<UUID, List<UUID>> hiddenPlayers = new HashMap<>();

    private void hidePlayer( Player a, Player b )
    {
        if ( !a.canSee( b ) )
        {
            return;
        }
        List<UUID> list;
        if ( hiddenPlayers.containsKey( a.getUniqueId() ) )
        {
            list = hiddenPlayers.get( a.getUniqueId() );
        }
        else
        {
            list = new ArrayList<>();
        }
        list.add( b.getUniqueId() );
        a.hidePlayer( b );
        hiddenPlayers.put( a.getUniqueId(), list );
    }

    private void showPlayer( Player a, Player b )
    {
        if ( !hiddenPlayers.containsKey( a.getUniqueId() ) )
        {
            return;
        }
        List<UUID> list = hiddenPlayers.get( a.getUniqueId() );
        if ( list.contains( b.getUniqueId() ) )
        {
            list.remove( b.getUniqueId() );
            a.showPlayer( b );
            hiddenPlayers.put( a.getUniqueId(), list );
        }
    }

    /**
     * Retrieve the look at vector.
     *
     * @param loc    - initial position. This vector will be modified.
     * @param lookat - the position to look at.
     * @return The look at vector.
     */
    private Location lookAt( Location loc, Location lookat )
    {
        double dx = lookat.getX() - loc.getX();
        double dy = lookat.getY() - loc.getY();
        double dz = lookat.getZ() - loc.getZ();

        double dxz = Math.sqrt( dx * dx + dz * dz );
        double pitch = Math.atan( dy / dxz );
        double yaw = 0;

        if ( dx != 0 )
        {
            if ( dx < 0 )
            {
                yaw = 1.5 * Math.PI;
            }
            else
            {
                yaw = 0.5 * Math.PI;
            }
            yaw -= Math.atan( dz / dx );
        }
        else if ( dz < 0 )
        {
            yaw = Math.PI;
        }

        loc.setYaw( (float) Math.toDegrees( -yaw ) );
        loc.setPitch( (float) Math.toDegrees( -pitch ) );
        return loc;
    }

    private Block getTargetBlock( Location direction, int maxDistance )
    {
        for ( Iterator<Block> it = new BlockIterator( direction, 0, maxDistance ); it.hasNext(); )
        {
            Block block = it.next();

            // Determine if this is a non-air block
            if ( !block.isEmpty() && !block.isLiquid() && !TRANSPARENT_MATERIALS.contains( block.getType().getId() ) )
            {
                return block;
            }
        }
        // No target block found
        return null;
    }

    private static final HashSet<Integer> TRANSPARENT_MATERIALS = new HashSet<>();

    static
    {
        TRANSPARENT_MATERIALS.add( Material.AIR.getId() );
        TRANSPARENT_MATERIALS.add( Material.SAPLING.getId() );
        TRANSPARENT_MATERIALS.add( Material.POWERED_RAIL.getId() );
        TRANSPARENT_MATERIALS.add( Material.DETECTOR_RAIL.getId() );
        TRANSPARENT_MATERIALS.add( Material.LONG_GRASS.getId() );
        TRANSPARENT_MATERIALS.add( Material.DEAD_BUSH.getId() );
        TRANSPARENT_MATERIALS.add( Material.YELLOW_FLOWER.getId() );
        TRANSPARENT_MATERIALS.add( Material.RED_ROSE.getId() );
        TRANSPARENT_MATERIALS.add( Material.BROWN_MUSHROOM.getId() );
        TRANSPARENT_MATERIALS.add( Material.RED_MUSHROOM.getId() );
        TRANSPARENT_MATERIALS.add( Material.TORCH.getId() );
        TRANSPARENT_MATERIALS.add( Material.REDSTONE_WIRE.getId() );
        TRANSPARENT_MATERIALS.add( Material.SEEDS.getId() );
        TRANSPARENT_MATERIALS.add( Material.SIGN_POST.getId() );
        TRANSPARENT_MATERIALS.add( Material.WOODEN_DOOR.getId() );
        TRANSPARENT_MATERIALS.add( Material.LADDER.getId() );
        TRANSPARENT_MATERIALS.add( Material.RAILS.getId() );
        TRANSPARENT_MATERIALS.add( Material.WALL_SIGN.getId() );
        TRANSPARENT_MATERIALS.add( Material.LEVER.getId() );
        TRANSPARENT_MATERIALS.add( Material.STONE_PLATE.getId() );
        TRANSPARENT_MATERIALS.add( Material.IRON_DOOR_BLOCK.getId() );
        TRANSPARENT_MATERIALS.add( Material.WOOD_PLATE.getId() );
        TRANSPARENT_MATERIALS.add( Material.REDSTONE_TORCH_OFF.getId() );
        TRANSPARENT_MATERIALS.add( Material.REDSTONE_TORCH_ON.getId() );
        TRANSPARENT_MATERIALS.add( Material.STONE_BUTTON.getId() );
        TRANSPARENT_MATERIALS.add( Material.SNOW.getId() );
        TRANSPARENT_MATERIALS.add( Material.SUGAR_CANE_BLOCK.getId() );
        TRANSPARENT_MATERIALS.add( Material.DIODE_BLOCK_OFF.getId() );
        TRANSPARENT_MATERIALS.add( Material.DIODE_BLOCK_ON.getId() );
        TRANSPARENT_MATERIALS.add( Material.PUMPKIN_STEM.getId() );
        TRANSPARENT_MATERIALS.add( Material.MELON_STEM.getId() );
        TRANSPARENT_MATERIALS.add( Material.VINE.getId() );
        TRANSPARENT_MATERIALS.add( Material.FENCE_GATE.getId() );
        TRANSPARENT_MATERIALS.add( Material.WATER_LILY.getId() );
        TRANSPARENT_MATERIALS.add( Material.NETHER_WARTS.getId() );
        TRANSPARENT_MATERIALS.add( Material.CARPET.getId() );
        TRANSPARENT_MATERIALS.add( Material.WATER.getId() );
        TRANSPARENT_MATERIALS.add( Material.STATIONARY_WATER.getId() );
        TRANSPARENT_MATERIALS.add( Material.GLASS.getId() );
        TRANSPARENT_MATERIALS.add( Material.STAINED_GLASS.getId() );
        TRANSPARENT_MATERIALS.add( Material.THIN_GLASS.getId() );
        TRANSPARENT_MATERIALS.add( Material.STAINED_GLASS_PANE.getId() );
    }

    @Override
    public void onDisable()
    {

    }
}
