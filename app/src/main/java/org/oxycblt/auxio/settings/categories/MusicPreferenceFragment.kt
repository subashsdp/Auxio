/*
 * Copyright (c) 2023 Auxio Project
 * MusicPreferenceFragment.kt is part of Auxio.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
 
package org.oxycblt.auxio.settings.categories

import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import coil3.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.oxycblt.auxio.R
import org.oxycblt.auxio.music.MusicSettings
import org.oxycblt.auxio.music.MusicViewModel
import org.oxycblt.auxio.settings.BasePreferenceFragment
import org.oxycblt.auxio.settings.SettingsBackupManager
import org.oxycblt.auxio.settings.ui.WrappedDialogPreference
import org.oxycblt.auxio.util.navigateSafe
import org.oxycblt.auxio.util.showToast
import org.oxycblt.musikr.tag.interpret.MetadataSanitizer
import timber.log.Timber as L

/**
 * "Content" settings.
 *
 * @author Alexander Capehart (OxygenCobalt)
 */
@AndroidEntryPoint
class MusicPreferenceFragment : BasePreferenceFragment(R.xml.preferences_music) {
    private val musicModel: MusicViewModel by viewModels()
    @Inject lateinit var imageLoader: ImageLoader
    @Inject lateinit var musicSettings: MusicSettings

    private val exportSettingsLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) {
                val result = SettingsBackupManager.exportSettings(requireContext(), uri)
                if (result.isSuccess) {
                    requireContext().showToast(R.string.set_export_success)
                } else {
                    requireContext().showToast(R.string.set_export_failed)
                }
            }
        }

    private val importSettingsLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                val result = SettingsBackupManager.importSettings(requireContext(), uri)
                if (result.isSuccess) {
                    MetadataSanitizer.updateSettings(
                        musicSettings.customExclusions,
                        musicSettings.customArtistMerges,
                    )
                    musicModel.refresh()
                    requireContext().showToast(R.string.set_import_success)
                } else {
                    requireContext().showToast(R.string.set_import_failed)
                }
            }
        }

    override fun onOpenDialogPreference(preference: WrappedDialogPreference) {
        if (preference.key == getString(R.string.set_key_separators)) {
            L.d("Navigating to separator dialog")
            findNavController().navigateSafe(MusicPreferenceFragmentDirections.separatorsSettings())
        }
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            getString(R.string.set_key_export_settings) -> {
                exportSettingsLauncher.launch(SettingsBackupManager.generateBackupFileName())
                return true
            }
            getString(R.string.set_key_import_settings) -> {
                importSettingsLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                return true
            }
            else -> return super.onPreferenceTreeClick(preference)
        }
    }

    override fun onSetupPreference(preference: Preference) {
        if (preference.key == getString(R.string.set_key_custom_exclusions)) {
            preference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                MetadataSanitizer.updateSettings(newValue as? String, musicSettings.customArtistMerges)
                musicModel.refresh()
                true
            }
        }
        if (preference.key == getString(R.string.set_key_custom_artist_merges)) {
            preference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
                MetadataSanitizer.updateSettings(musicSettings.customExclusions, newValue as? String)
                musicModel.refresh()
                true
            }
        }
        if (preference.key == getString(R.string.set_key_cover_mode)) {
            L.d("Configuring cover mode setting")
            preference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
                L.d("Cover mode changed, reloading music")
                musicModel.refresh()
                true
            }
        }
        if (preference.key == getString(R.string.set_key_square_covers)) {
            L.d("Configuring square cover setting")
            preference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
                L.d("Cover mode changed, resetting image memory cache")
                imageLoader.memoryCache?.clear()
                true
            }
        }
        if (preference.key == getString(R.string.set_key_with_hidden)) {
            L.d("Configuring ignore hidden files setting")
            preference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, _ ->
                L.d("Ignore hidden files setting changed, reloading music")
                musicModel.refresh()
                true
            }
        }
    }
}
