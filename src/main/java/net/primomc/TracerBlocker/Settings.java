package net.primomc.TracerBlocker;

import java.util.ArrayList;
import java.util.List;

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
public class Settings
{
    static class PlayerHider
    {
        public static boolean enabled = false;
        public static int everyTicks = 1;
        public static int ignoreDistance = 7;
        public static int maxDistance = 120;
        public static List<String> disabledWorlds = new ArrayList<>();
    }

    static class ChestHider
    {
        public static boolean enabled = false;
        public static int everyTicks = 3;
        public static int ignoreDistance = 7;
        public static int maxDistance = 80;
        public static List<String> disabledWorlds = new ArrayList<>();
    }

    static class FakePlayers
    {
        public static boolean enabled = false;
        public static boolean moving = true;
        public static int everyTicks = 40;
        public static int secondsAlive = 5;
        public static int speed = 3;
        public static List<String> disabledWorlds = new ArrayList<>();
    }
}
