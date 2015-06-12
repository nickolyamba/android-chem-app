import webapp2
from google.appengine.ext import ndb
import db_models
import json
import base64
from apiclient.discovery import build
import hashlib

class Supplier(webapp2.RequestHandler):
	def respond(self, code, message, fix=None, dev_mess=None, user_mess=None):
		self.response.status = code
		self.response.status_message = message
		out = {}
		out['developerMessage'] = dev_mess
		out['userMessage'] = user_mess # optional if error msg is not straight enough
		out['errorCode'] = code
		out['errorMessage'] = str(code) + ' ' + message
		out['fix_url'] = fix
		self.response.write(json.dumps(out))

	def root_key(self, USER_ID):
		return ndb.Key("User", USER_ID)
	
	# Edit Cache-Control and Content-Type HTTP eaders
	def headers(self):
		#http://stackoverflow.com/questions/8550066/how-to-use-cache-control-with-python-in-gae
		seconds_valid = 60
		self.response.headers['Cache-Control'] = "public, max-age=%d" % seconds_valid
		self.response.headers['Content-Type'] = "application/json"

	def supplier_by_key(self, ident, USER_ID):
		# find the Supplier object to Update
		if ident.isdigit():
			supplier = ndb.Key(db_models.Supplier, int(ident), parent=self.root_key(USER_ID)).get()
			if supplier:
				return supplier
			else:
				self.respond(404, "Not Found. No suppliers with id = " + str(ident) + " found",
							'http://localhost:17080/supplier')
				return 0
		else:
			self.respond(404, "No ID provided. ID must be supplier's name or key", 'http://localhost:17080/supplier')
			return 0

	def deleteChem(self, supplier_key):
		''' !!! When Supplier is Chemical's parent !!!
		chemicals = db_models.Chemical.query(ancestor=supplier_key).fetch()
		results = [chem for chem in chemicals]
		output = [] # here will be list of chemicals data
		out = {} # dictionary
		for chem in results:
			result = chem.to_dict()
			output.append(result)
			out['chemicals'] = output
			#delete chemical
			chem.key.delete()
		#self.response.write(json.dumps(out))
		'''
		#ancestor=self.root_key(USER_ID)
		chemicals = db_models.Chemical.query(db_models.Chemical.supplier == supplier_key).fetch()
		#self.response.write(sols)
		results = [chem for chem in chemicals]
		output = [] # here will be list of chemicals data
		out = {} # dictionary
		for chem in results:
			result = chem.to_dict()
			output.append(result)
			out['chemicals'] = output
			#delete chemical
			chem.key.delete()
		return out

	#http://stackoverflow.com/questions/1499832/
	def get_auth(self):	  
		USER_ID = None
		TOKEN = None
		if not 'Authorization' in self.request.headers:
			self.response.headers['WWW-Authenticate'] = 'Basic realm="MYREALM"'
			self.response.set_status(401)
			#self.response.out.write("Authorization required")
			self.respond(401, "Authorization required")
		else:
			auth = self.request.headers['Authorization']
			(USER_ID, TOKEN) = base64.b64decode(auth.split(' ')[1]).split(':')
		# Check the username and password, and proceed ...
		return USER_ID, TOKEN

	def authentificate(self, USER_ID, TOKEN):
		# check if user exist in the table
		USER_ID_utf = USER_ID.encode('utf-8')
		TOKEN_utf = TOKEN.encode('utf-8')
		user_in_table = ndb.Key(db_models.User, USER_ID_utf).get()
		if not user_in_table:
			return "noUser"

	def post(self):
		"""
			Creates a Supplier entity

			POST Body Varibales:
			name - required String
			website - optional String
		curl --include --data "name=Aldrich&website=http://www.aldrich.com" -H "Accept: application/json" http://localhost:20080/supplier
		"""
		# Add appropriate headers info in HTTP response
		self.headers()

		if 'application/json' not in self.request.accept:
			self.respond(406, "Not Acceptable. API only supports application/json MIME type")
			return

		(USER_ID, TOKEN) = self.get_auth()
		#self.response.write(USER_ID + TOKEN)
		
		if USER_ID and TOKEN:
			is_auth = self.authentificate(USER_ID, TOKEN)
			if is_auth == "noUser":
				self.respond(401, "NOT Authorized! Check your credentials and sign-in with Google account")
				return
		else:
			self.respond(401, "NOT Authorized! Check your credentials and sign-in with Google account")
			return
		
		name = self.request.get('name', default_value=None)
		website = self.request.get('website', default_value=None)
			
		if name:
			# Check if this Supplier already exists
			suppliers = db_models.Supplier.query(ancestor=self.root_key(USER_ID)).fetch(projection=["name"])
			for supp in suppliers:
					if name == supp.name:
						self.respond(400, "Bad Request. Supplier " + supp.name + " already exists and is not added again")
						return
			
			# If supplier new, add it
			supplier = db_models.Supplier(parent=self.root_key(USER_ID))
			supplier.name = name	
		
		else:
			self.respond(400, "Invalid Request. Supplier name is required")
			return

		# add website if there is one
		if website:
			supplier.website = website

		key = supplier.put()
		out = supplier.to_dict()
		self.response.write(json.dumps(out))
		
		return

	
	def get(self, **kwargs):
		"""
			Get a Supplier entity by id, or 
			if there is no such id, returns all Supplier entities - list of suppliers

			GET Varibales:
			id - String

			GET all suppliers:
			curl --include -H "Accept: application/json" http://localhost:20080/supplier

			GET one supplier by its id:
			curl --include -H "Accept: application/json" http://localhost:20080/supplier/5066549580791808
		"""
		# initialize response HTTP headres
		self.headers()

		if 'application/json' not in self.request.accept:
			self.respond(406, "Not Acceptable. API only supports application/json MIME type")
			return

		(USER_ID, TOKEN) = self.get_auth()
		#self.response.write(USER_ID + TOKEN)
		
		if USER_ID and TOKEN:
			is_auth = self.authentificate(USER_ID, TOKEN)
			if is_auth == "noUser":
				self.respond(401, "NOT Authorized! Check your credentials and sign-in with Google account")
				return
		else:
			self.respond(401, "NOT Authorized! Check your credentials and sign-in with Google account")
			return

		supplier = db_models.Supplier()
		
		if 'id' in kwargs:
			supplier = self.supplier_by_key(kwargs['id'], USER_ID)
			if supplier:
				# print found supplier
				output = supplier.to_dict()
				self.response.write(json.dumps(output))
			else:
				return

		# if there is no id argument in kwargs, return all suppliers list with keys
		# # Query for list of all suppliers and return the list in json
		else:
			q = db_models.Supplier.query(ancestor=self.root_key(USER_ID)).fetch()
			results = [x for x in q]
			output = [] # here will be list of suppliers data
			out = {} # dictionary
			for x in results:
				result = x.to_dict()
				output.append(result)
			out['suppliers'] = output
			self.response.write(json.dumps(out))


	def put(self, **kwargs):
		"""
			Update suppliers entity by its name

			PUT Body Varibales:
			new name - String (optional)
			new website - String (optional)

			Example:
			curl -i -X PUT -d "name=Amazon&website=http://www.amazon.com" -H "Accept: application/json" 
			http://localhost:20080/supplier/5066549580791808
		"""
		# initialize response HTTP headres
		self.headers()

		if "application/json" not in self.request.accept:
			self.respond(406, "Not Acceptable. API only supports application/json MIME type")
			return

		(USER_ID, TOKEN) = self.get_auth()
		#self.response.write(USER_ID + TOKEN)
		
		if USER_ID and TOKEN:
			is_auth = self.authentificate(USER_ID, TOKEN)
			if is_auth == "noUser":
				self.respond(401, "NOT Authorized! Check your credentials and sign-in with Google account")
				return
		else:
			self.respond(401, "NOT Authorized! Check your credentials and sign-in with Google account")
			return

		# get new values to assign to supplier object from request data
		name = self.request.get('name', default_value=None)
		website = self.request.get('website', default_value=None)

		supplier = db_models.Supplier()
		
		if 'id' in kwargs:
			supplier = self.supplier_by_key(kwargs['id'], USER_ID)

		if not supplier:
			return
		
		# update supplier entity	
		if name:
			#q = q.filter(db_models.Supplier.name == name)
			supplier.name = name
		
		if website:
			#q = q.filter(db_models.Supplier.website == website)
			supplier.website = website
		
		if not name and not website:
				self.respond(404, "Not Found. To search enter name or website as a filter")
				return

		# add Supplier object to database and return it as JSON
		key = supplier.put()
		out = supplier.to_dict()
		self.response.write(json.dumps(out))


	def delete(self, **kwargs):
		"""
			Delete a Supplier entity by id and Delete supplier from Chemical if there is one

			Delete Varibales:
			id - String

			Example:
			curl -i -X DELETE -H "Accept: application/json" /
			https://cs496-final-proj.appspot.com/supplier/5634387206995968
		"""
		# initialize response HTTP headres
		self.headers()

		if 'application/json' not in self.request.accept:
			self.respond(406, "Not Acceptable. API only supports application/json MIME type")
			return

		(USER_ID, TOKEN) = self.get_auth()
		#self.response.write(USER_ID + TOKEN)
		
		if USER_ID and TOKEN:
			is_auth = self.authentificate(USER_ID, TOKEN)
			if is_auth == "noUser":
				self.respond(401, "NOT Authorized! Check your credentials and sign-in with Google account")
				return
		else:
			self.respond(401, "NOT Authorized! Check your credentials and sign-in with Google account")
			return

		supplier = db_models.Supplier()
		
		if 'id' in kwargs:
			# get Supplier entity by its key
			supplier = self.supplier_by_key(kwargs['id'], USER_ID)
			if supplier:
				# delete this key in Solution that has this chemical
				#suppID = ndb.Key(db_models.Supplier, int(kwargs['id']), parent=self.root_key()).get()
				# delete any chemical entity that references this supplier, too, since supplier is required
				del_entities = []
				supp_list = []
				supp_dict = {}
				chem_dict = self.deleteChem(supplier.key)
				supp_out = supplier.to_dict()
				supp_list.append(supp_out)
				supp_dict['suppliers'] = supp_list
				# delete Supplier entitity
				supplier.key.delete()
				# print deleted supplier and chemical entities
				del_entities.append(chem_dict)
				del_entities.append(supp_dict)
				self.response.write(json.dumps(del_entities))
			else:
				return

		# if there is no id argument in kwargs, return all suppliers list with keys
		# # Query for list of all suppliers and return the list in json
		else:
			self.respond(404, "No ID provided. ID must be supplier's name or key", 
									'http://localhost:17080/supplier',
									"Follow fix_url to get the list of all suppliers")
			return
	

class SupplierSearch(webapp2.RequestHandler):
	def respond(self, code, message, fix=None, dev_mess=None, user_mess=None):
		self.response.status = code
		self.response.status_message = message
		out = {}
		out['developerMessage'] = dev_mess
		out['userMessage'] = user_mess
		out['errorCode'] = code
		out['errorMessage'] = str(code) + ' ' + message
		out['fix_url'] = fix
		self.response.write(json.dumps(out))

	# default Supplier's key
	def root_key(self):
		return ndb.Key("Root", "default")

	# Edit Cache-Control and Content-Type HTTP eaders
	def headers(self):
		seconds_valid = 60
		self.response.headers['Cache-Control'] = "public, max-age=%d" % seconds_valid
		self.response.headers['Content-Type'] = "application/json"

	def post(self, **kwargs):
		"""
			Search for suppliers filtering by name and/or website

			POST Body Varibales:
			name - String
			website - String (optional)

			curl --include --data "name=Aldrich" -H "Accept: application/json" http://localhost:20080/supplier/search
		"""
		self.headers()

		#limit = 10
		#offset = 0

		if "application/json" not in self.request.accept:
			self.respond(406, "Not Acceptable. API only supports application/json MIME type")
			return

		# query for all supplier that have ancestor key = self.root_key() 
		q = db_models.Supplier.query(ancestor=self.root_key())
		
		name = self.request.get('name', default_value=None)
		website = self.request.get('website', default_value=None)
			
		if name:
			q = q.filter(db_models.Supplier.name == name)
		
		if website:
			q = q.filter(db_models.Supplier.website == website)
		
		if not name and not website:
				self.respond(404, "Not Found. To search enter name or website as a filter")
				return

		# fetch and show keys only
		keys = q.fetch(keys_only=True)
		results = {'keys': [x.id() for x in keys]}
		self.response.write(json.dumps(results))


	def put(self, **kwargs):
		"""
			Update supplier entity by its name

			PUT Body Varibales:
			new name - String
			new website - String optional

			Example:
			curl -i -X PUT -d "name=Amazon&website=http://www.amazon.com" -H "Accept: application/json" 
			http://localhost:20080/supplier/Aldrich

			Output: new Supplier entity
			{"key": 5629499534213120, "name": "Aldrich", "website": "http://www.aldrich.com"}
		"""
		# initialize response HTTP headres
		self.headers()

		if "application/json" not in self.request.accept:
			self.respond(406, "Not Acceptable. API only supports application/json MIME type")
			return

		# get new values to assign to supplier object from request data
		name = self.request.get('name', default_value=None)
		website = self.request.get('website', default_value=None)

		# find the Supplier object to Update
		if 'id' in kwargs:
			# if digit
			if (kwargs['id']):
				# ancestor query for instance of supplier: 
				# https://cloud.google.com/appengine/docs/python/ndb/queries#ancestor
				suppliers = db_models.Supplier.query(db_models.Supplier.name == kwargs['id'], ancestor=self.root_key()).fetch()
				if len(suppliers) == 1:
					supplier = suppliers[0]
					#self.response.write(supplier)
					#self.response.write('\n\n')
					#output = supplier.to_dict()
					#self.response.write(json.dumps(output))
				# cannot be > 1 because duplicates checked on POST, therefore can be only 0
				else:
					self.respond(404, "Not Found. No suppliers with name = " + kwargs['id'] + " found",
								'http://localhost:17080/supplier', 
								"Follow fix_url to get the list of all suppliers",
								"Check your data synchronization with the cloud")
					return
			else:
				self.respond(404, "No ID provided. ID must be supplier's name or key", 
									'http://localhost:17080/supplier',
									"Follow fix_url to get the list of all suppliers",)
				return
		
		# update supplier entity	
		if name:
			supplier.name = name

		if website:
			supplier.website = website
		
		if not name and not website:
				self.respond(404, "Not Found. To search enter name or website as a filter")
				return

		# add Supplier object to database and return it as JSON
		key = supplier.put()
		out = supplier.to_dict()
		self.response.write(json.dumps(out))