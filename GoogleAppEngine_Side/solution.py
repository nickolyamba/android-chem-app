import webapp2
from datetime import datetime
from google.appengine.ext import ndb
import db_models
import json

class Solution(webapp2.RequestHandler):
	
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

	def root_key(self):
		return ndb.Key("Root", "default")
	
	# Edit Cache-Control and Content-Type HTTP eaders
	def headers(self):
		#http://stackoverflow.com/questions/8550066/how-to-use-cache-control-with-python-in-gae
		seconds_valid = 60
		self.response.headers['Cache-Control'] = "public, max-age=%d" % seconds_valid
		self.response.headers['Content-Type'] = "application/json"

	def solution_by_key(self, ident):
		# find the Supplier object to Update
		if ident.isdigit():
			solution = ndb.Key(db_models.Solution, int(ident), parent=self.root_key()).get()
			if solution:
				return solution
			else:
				self.respond(404, "Not Found. No solutions with id = " + str(ident) + " found",
							'http://localhost:17080/solution')
				return 0
		else:
			self.respond(404, "No ID provided. ID must be solution's name or key", 'http://localhost:17080/solution')
			return 0
	
	def isDuplicate(self, solutionID):
		sols = db_models.Solution.query().fetch(projection=["solutionID"])
		for sol in sols:
			if solutionID == sol.solutionID:
				return True

	def post(self):
		'''
			Creates a Solution entity

			POST Body Varibales

			name - required
			volume - required key
			chemicals - array of keys of chemicals
			date - date required
		'''
		self.headers()

		if 'application/json' not in self.request.accept:
			self.respond(406, "Not Acceptable. API only supports application/json MIME type")
			return
		
		solutionID = self.request.get('solutionID', default_value=None)
		name = self.request.get('name', default_value=None)
		volume = self.request.get('volume', default_value=None)
		chemicals = self.request.get_all('chemicals[]', default_value=None)
		#date = self.request.get('date', default_value=None)
		
		# check if there is already solution with this ID
		if solutionID:
			if solutionID.isdigit():
				solutionID = int(solutionID)
				if self.isDuplicate(solutionID):
					self.respond(400, "Bad Request. Solution " + str(solutionID) + " already exists and is not added again")
					return
			else:
				self.respond(400, "Invalid Request. solutionID must be number",
													 "http://localhost:17080/chemical",
										"Follow fix_url to see the list of all solutions")
				return
		else:
			self.respond(400, "Invalid Request. solutionID is required",
										"http://localhost:17080/chemical",
										"Follow fix_url to see the list of all solutions")
			return

		# parent key solution is a key of the first chemical in the array
		# create solution if chemical key is valid
		if chemicals:
			# will also fail if empty
			if (chemicals[0]).isdigit():
				chemical_key = ndb.Key(db_models.Chemical, int(chemicals[0]))
			else:
				self.respond(400, "Invalid Request. Chemicals id must be number",
											'http://localhost:17080/chemical',
											"Follow fix_url to see the list of all chemicals")
				return
		else:
				self.respond(400, "Invalid Request. Chemicals id is required",
											"http://localhost:17080/chemical",
											"Follow fix_url to see the list of all chemicals")
				return

		#solution = db_models.Solution(parent=chemical_key)
		solution = db_models.Solution(parent=self.root_key())

		# add chemicals
		if chemicals:
			for chemical in chemicals:
				if chemical.isdigit():
					solution.chemicals.append(ndb.Key(db_models.Chemical, int(chemical)))
				else:
					self.respond(400, "Invalid Request. Chemical id must exist and be number",
												"http://localhost:17080/chemical",
												"Follow fix_url to see the list of all chemicals")
					return

		# add solutionID - already validated if got here
		solution.solutionID = int(solutionID)

		# add name
		if name:
			solution.name = name	
		else:
			self.respond(400, "Invalid Request. Solution name is required",
									"http://localhost:17080/chemical",
									"See the list of all chemicals to come up with a good name")
			return
		
		# add volume
		if volume:
			if volume.isdigit():
				solution.volume = int(volume)	
			else:
				self.respond(400, "Invalid Request. Volume is must be number")
				return
		else:
				self.respond(400, "Invalid Request. Volume is required")
				return
		
		key = solution.put()
		out = solution.to_dict()
		self.response.write(json.dumps(out))
		
		return


	def get(self):
		'''
			Returns a List of Solution entities

			GET 
			None args required
		'''
		self.headers()

		if 'application/json' not in self.request.accept:
			self.respond(406, "Not Acceptable. API only supports application/json MIME type")
			return
		# Query for list of all solutions and return in json
		q = db_models.Solution.query().fetch()
		results = [x for x in q]
		output = []
		for x in results:
			result = x.to_dict()
			output.append(result)
		self.response.write(json.dumps(output))
		
		return


	def delete(self, **kwargs):
		"""
			Delete a Solution entity by id

			Delete Varibales:
			id - String

			Example:
			curl -i -X DELETE -H "Accept: application/json" /
			https://cs496-final-proj.appspot.com/solution/5768037999312896

			{"solutions": [{"volume": 25, "key": 5768037999312896, 
			"chemicals": [5838406743490560], "name": "Solution #1", "solutionID": 1}]}
		"""
		# initialize response HTTP headres
		self.headers()

		if 'application/json' not in self.request.accept:
			self.respond(406, "Not Acceptable. API only supports application/json MIME type")
			return

		solution = db_models.Supplier()
		
		if 'id' in kwargs:
			# get Supplier entity by its key
			solution = self.solution_by_key(kwargs['id'])
			if solution:
				# delete Supplier entitity
				#solution.key.delete()
				# create JSON response
				del_entities = []
				supp_list = []
				supp_dict = {}
				supp_out = solution.to_dict()
				supp_list.append(supp_out)
				supp_dict['solutions'] = supp_list

				# print deleted solution and chemical entities
				self.response.write(json.dumps(supp_dict))
			else:
				return

		# if there is no id argument in kwargs, return all solutions list with keys
		# # Query for list of all solutions and return the list in json
		else:
			self.respond(404, "No ID provided. ID must be solution's name or key", 
									'http://localhost:17080/solution',
									"Follow fix_url to get the list of all solutions")
			return
		

class DeleteSolution(webapp2.RequestHandler):
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

	def root_key(self):
		return ndb.Key("Root", "default")
	
	# Edit Cache-Control and Content-Type HTTP eaders
	def headers(self):
		#http://stackoverflow.com/questions/8550066/how-to-use-cache-control-with-python-in-gae
		seconds_valid = 60
		self.response.headers['Cache-Control'] = "public, max-age=%d" % seconds_valid
		self.response.headers['Content-Type'] = "application/json"
	'''
		Delete a Solution entity

		PUT
		name - required
		supplier - required key

	'''