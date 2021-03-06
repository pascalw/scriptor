import Ember from 'ember';
import config from './config/environment';

var Router = Ember.Router.extend({
  location: config.locationType
});

Router.map(function() {
  this.route('endpoints', function() {
    this.route('show', { path: ':id' });
    this.route('new');
  });
  this.route('endpoints', { path: '/' });
  this.route('setup');
});

export default Router;
