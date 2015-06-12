import webapp2_extras.auth
from apiclient.discovery import build
import webapp2
from google.appengine.ext import ndb
import db_models
import json
from oauth2client import client, crypt
import hashlib

CLIENT_ID = "****"
ANDROID_CLIENT_ID = "********"
WEB_CLIENT_ID = "********"
# http://blog.abahgat.com/2013/01/07/user-authentication-with-webapp2-on-google-app-engine/
# (Receive token by HTTPS POST)
class GetToken(webapp2.RequestHandler):
	def post(self):
		token = self.request.get('idToken', default_value=None)
		#https://developers.google.com/identity/sign-in/android/backend-auth
		try:
			out = build('oauth2', 'v1').tokeninfo(access_token=token).execute()
			#auth_json = json.dumps(out)
			USER_ID = out['user_id']
			# If supplier new, add it
			token_utf = token.encode('utf-8')
			hash_token = hashlib.md5(token_utf).hexdigest()
			#User user = get_by_id(hash_token)
			user = db_models.User(userID=USER_ID, hashToken=hash_token, id=USER_ID)
			user.put()
		#self.redirect(self.uri_for('home')
		except crypt.AppIdentityError:
			None
		self.response.write(json.dumps(out))
		#self.redirect(self.uri_for('home'))