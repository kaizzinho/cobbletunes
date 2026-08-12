package com.kaizzinho.cobbletunes.event

import com.cobblemon.mod.common.api.battles.model.actor.BattleActor
import com.cobblemon.mod.common.api.battles.model.actor.EntityBackedBattleActor
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.kaizzinho.cobbletunes.LOGGER
import net.fabricmc.loader.api.FabricLoader
import java.lang.reflect.Method
import java.util.UUID

object WildBossesBridge {
    private const val MOD_ID = "wildbosses"
    private const val API_CLASS = "com.kaizzinho.wildbosses.api.WildBossIntegrationApi"
    private var compatibilityFailureLogged = false

    private data class ApiMethods(
        val entityTier: Method,
        val pokemonTier: Method
    )

    private val apiMethods: ApiMethods? by lazy {
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) return@lazy null

        runCatching {
            val api = Class.forName(API_CLASS)
            ApiMethods(
                entityTier = api.getMethod("getTierName", PokemonEntity::class.java),
                pokemonTier = api.getMethod("getTierNameByPokemonUuid", UUID::class.java)
            )
        }.getOrElse {
            logCompatibilityFailure(it)
            null
        }
    }

    fun resolveTier(opposingActors: List<BattleActor>): String? {
        val methods = apiMethods ?: return null

        for (actor in opposingActors) {
            val entity = (actor as? EntityBackedBattleActor<*>)?.entity as? PokemonEntity
            val entityTier = entity?.let {
                invokeTier(methods.entityTier, it)
            }
            if (entityTier != null) return entityTier

            for (battlePokemon in actor.pokemonList) {
                val tier = invokeTier(methods.pokemonTier, battlePokemon.effectedPokemon.uuid)
                if (tier != null) return tier
            }
        }

        return null
    }

    private fun invokeTier(method: Method, argument: Any): String? =
        runCatching { method.invoke(null, argument) as? String }
            .getOrElse {
                logCompatibilityFailure(it)
                null
            }
            ?.takeIf { it.isNotBlank() }
            ?.uppercase()

    private fun logCompatibilityFailure(error: Throwable) {
        if (compatibilityFailureLogged) return
        compatibilityFailureLogged = true
        LOGGER.warn(
            "[cobbletunes] WildBosses integration disabled: {}",
            error.cause?.message ?: error.message ?: error.javaClass.simpleName
        )
    }
}
