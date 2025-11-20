package com.ganwooma.randomTP

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import kotlin.random.Random

class RandomTP : JavaPlugin() {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {

        if (!command.name.equals("randomtp", ignoreCase = true)) return false

        if (!sender.isOp) {
            sender.sendMessage("§6[ RandomTP ] §cOP만 사용 가능합니다.")
            return true
        }

        if (args.size < 3) {
            sender.sendMessage("§6[ RandomTP ] §c사용법: /randomtp <centerX> <centerZ> <radius>")
            return true
        }

        val cx = args[0].toIntOrNull()
        val cz = args[1].toIntOrNull()
        val radius = args[2].toIntOrNull()

        if (cx == null || cz == null || radius == null || radius <= 0) {
            sender.sendMessage("§6[ RandomTP ] §c숫자를 정확히 입력해주세요.")
            return true
        }

        val world = Bukkit.getWorld("world")
        if (world == null) {
            sender.sendMessage("§6[ RandomTP ] §c월드를 찾을 수 없습니다.")
            return true
        }

        sender.sendMessage("§6[ RandomTP ] §b랜덤 좌표로 이동합니다...")

        for (player in Bukkit.getOnlinePlayers()) {
            findSafeLocationAsync(world, cx, cz, radius, player, 0)
        }

        return true
    }


    private fun findSafeLocationAsync(
        world: org.bukkit.World,
        centerX: Int,
        centerZ: Int,
        radius: Int,
        player: Player,
        attempt: Int
    ) {
        if (attempt >= 20) {
            player.sendMessage("§6[ RandomTP ] §c안전한 좌표를 찾지 못했습니다.")
            return
        }

        val randomX = centerX + Random.nextInt(-radius, radius + 1)
        val randomZ = centerZ + Random.nextInt(-radius, radius + 1)

        val chunkX = randomX shr 4
        val chunkZ = randomZ shr 4

        // 비동기로 chunk 로딩
        world.getChunkAtAsync(chunkX, chunkZ).thenAccept { chunk ->

            val highestY = world.getHighestBlockYAt(randomX, randomZ)
            if (highestY <= 0) {
                // 재시도
                findSafeLocationAsync(world, centerX, centerZ, radius, player, attempt + 1)
                return@thenAccept
            }

            val block = world.getBlockAt(randomX, highestY, randomZ)

            // 용암/공중 방지
            if (block.type == Material.LAVA || block.type == Material.FIRE) {
                // 재시도
                findSafeLocationAsync(world, centerX, centerZ, radius, player, attempt + 1)
                return@thenAccept
            }

            val finalLocation = Location(world, randomX + 0.5, highestY + 1.0, randomZ + 0.5)

            Bukkit.getScheduler().runTask(this@RandomTP, Runnable {
                player.teleport(finalLocation)
                // 침대를 사용하지 않은 경우 스폰포인트 설정
                if (player.bedSpawnLocation == null) {
                    player.setBedSpawnLocation(finalLocation, true)
                }
                player.sendMessage("§6[ RandomTP ] §a이동된 좌표 → §bX: ${finalLocation.blockX}, Y: ${finalLocation.blockY}, Z: ${finalLocation.blockZ}")
            })
        }
    }
}