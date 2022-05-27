import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MainModule } from '@main/_main.module';
import { SettingsPage } from 'src/app/settings/pages/settings/settings.page';
import { IntegrationEntryComponent } from './components/integration-entry/integration-entry.component';
import { ListGroupComponent } from './components/list-group/list-group.component';
import { SettingsAccountPage } from './pages/settings-account/settings-account.page';
import { SettingsIntegrationsPage } from './pages/settings-integrations/settings-integrations.page';
import { SettingsLocalizationPage } from './pages/settings-localization/settings-localization.page';
import { SettingsRoutingModule } from './settings.routing';

@NgModule({
  imports: [CommonModule, MainModule, ReactiveFormsModule, SettingsRoutingModule],
  declarations: [
    SettingsPage,
    SettingsLocalizationPage,
    SettingsAccountPage,
    SettingsIntegrationsPage,
    ListGroupComponent,
    IntegrationEntryComponent,
  ],
})
export class SettingsModule {}
