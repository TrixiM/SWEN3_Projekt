import { Routes } from '@angular/router';
import { DashboardComponent} from "./pages/dashboard/dashboard.component";
import { DetailComponent} from "./pages/detail/detail.component";

export const routes: Routes = [
  { path: '', component: DashboardComponent },
  { path: 'document/:id', component: DetailComponent },
];
